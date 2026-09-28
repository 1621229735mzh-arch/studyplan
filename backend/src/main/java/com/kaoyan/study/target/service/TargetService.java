package com.kaoyan.study.target.service;

import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.common.exception.NotFoundException;
import com.kaoyan.study.target.dto.TargetRequest;
import com.kaoyan.study.target.dto.TargetResponse;
import com.kaoyan.study.target.dto.TargetStatusRequest;
import com.kaoyan.study.target.entity.TargetLink;
import com.kaoyan.study.target.entity.TargetSchool;
import com.kaoyan.study.target.mapper.TargetMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 候选目标用例。
 *
 * <p>返回 {@link TargetResponse} 而非实体：链接必须和目标一起给出，组装放在业务层，
 * 接口层不再回头调用数据访问。
 */
@Service
public class TargetService {

    private static final String DEFAULT_STATUS = "CANDIDATE";
    private static final Set<String> ALLOWED_STATUSES = Set.of("CANDIDATE", "CHOSEN", "DROPPED");

    private final TargetMapper targetMapper;

    public TargetService(TargetMapper targetMapper) {
        this.targetMapper = targetMapper;
    }

    /**
     * 列表；status 为空表示不筛选，每个目标带上自己的链接。
     *
     * <p>链接逐条查询：候选目标数量是个位数级别，逐条取比批量拼装更容易看懂，
     * 暂不为它引入额外的关联查询。
     */
    public List<TargetResponse> list(String status) {
        String filter = normalizeStatus(status);
        return targetMapper.findAll(filter).stream()
                .map(target -> TargetResponse.from(target, targetMapper.findLinksByTargetId(target.getId())))
                .toList();
    }

    public TargetResponse get(Long id) {
        TargetSchool target = requireExisting(id);
        return TargetResponse.from(target, targetMapper.findLinksByTargetId(id));
    }

    @Transactional
    public TargetResponse create(TargetRequest request) {
        String status = request.status() == null ? DEFAULT_STATUS : requireValidStatus(request.status());
        TargetSchool target = new TargetSchool();
        apply(target, request);
        target.setStatus(status);
        target.setVersion(0L);
        targetMapper.insert(target);
        insertLinks(target.getId(), request.links());
        return get(target.getId());
    }

    /**
     * 修改目标并整体替换链接。
     *
     * <p>先按版本写入主体，0 行说明其他设备已改过，此时直接拒绝，绝不清空旧链接；
     * 版本通过后才先删后插，两步在同一事务内完成。
     */
    @Transactional
    public TargetResponse update(Long id, TargetRequest request) {
        TargetSchool existing = requireExisting(id);
        apply(existing, request);
        if (request.status() != null) {
            existing.setStatus(requireValidStatus(request.status()));
        }
        existing.setVersion(request.version());
        int updated = targetMapper.update(existing);
        if (updated == 0) {
            throw new ConflictException("TARGET_STALE", "该候选目标已在其他设备上修改，请刷新后重试");
        }
        replaceLinks(id, request.links());
        return get(id);
    }

    /** 只改状态：不触碰备注、专业与链接，版本检查与其他编辑一致。 */
    @Transactional
    public TargetResponse updateStatus(Long id, TargetStatusRequest request) {
        requireExisting(id);
        String status = requireValidStatus(request.status());
        int updated = targetMapper.updateStatus(id, status, request.version());
        if (updated == 0) {
            throw new ConflictException("TARGET_STALE", "该候选目标已在其他设备上修改，请刷新后重试");
        }
        return get(id);
    }

    /** 删除目标；链接由外键 ON DELETE CASCADE 连带删除，这里不单独发删除语句。 */
    @Transactional
    public void delete(Long id) {
        requireExisting(id);
        targetMapper.deleteById(id);
    }

    private TargetSchool requireExisting(Long id) {
        TargetSchool target = targetMapper.findById(id);
        if (target == null) {
            throw new NotFoundException("候选目标不存在");
        }
        return target;
    }

    private void apply(TargetSchool target, TargetRequest request) {
        target.setSchoolName(request.schoolName().trim());
        target.setMajorName(request.majorName());
        target.setNote(request.note());
        target.setSortOrder(request.sortOrderOrDefault());
    }

    /** 链接整体替换：先删后插，保证提交的顺序就是展示顺序。 */
    private void replaceLinks(Long targetId, List<TargetRequest.LinkRequest> links) {
        targetMapper.deleteLinksByTargetId(targetId);
        insertLinks(targetId, links);
    }

    private void insertLinks(Long targetId, List<TargetRequest.LinkRequest> links) {
        if (links == null || links.isEmpty()) {
            return;
        }
        int sortOrder = 0;
        for (TargetRequest.LinkRequest link : links) {
            TargetLink entity = new TargetLink();
            entity.setTargetId(targetId);
            entity.setUrl(link.url().trim());
            entity.setLabel(link.label());
            entity.setSortOrder(sortOrder++);
            targetMapper.insertLink(entity);
        }
    }

    /** 校验状态取值；返回 null 表示“未提供”，列表即不过滤、修改即保持原状态。 */
    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String value = status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(value)) {
            throw new ConflictException("TARGET_STATUS_INVALID", "目标状态只能是 CANDIDATE、CHOSEN 或 DROPPED");
        }
        return value;
    }

    private String requireValidStatus(String status) {
        String value = normalizeStatus(status);
        if (value == null) {
            throw new ConflictException("TARGET_STATUS_INVALID", "目标状态只能是 CANDIDATE、CHOSEN 或 DROPPED");
        }
        return value;
    }
}
