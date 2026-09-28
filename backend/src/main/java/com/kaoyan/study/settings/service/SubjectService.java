package com.kaoyan.study.settings.service;

import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.common.exception.NotFoundException;
import com.kaoyan.study.settings.dto.SubjectRequest;
import com.kaoyan.study.settings.entity.Subject;
import com.kaoyan.study.settings.mapper.SubjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 科目用例。 */
@Service
public class SubjectService {

    private final SubjectMapper subjectMapper;

    public SubjectService(SubjectMapper subjectMapper) {
        this.subjectMapper = subjectMapper;
    }

    public List<Subject> list(boolean enabledOnly) {
        return subjectMapper.findAll(enabledOnly);
    }

    public Subject get(Long id) {
        Subject subject = subjectMapper.findById(id);
        if (subject == null) {
            throw new NotFoundException("科目不存在");
        }
        return subject;
    }

    /** 供其他模块校验外键用；不存在时抛出 404。 */
    public Subject requireExisting(Long id) {
        return get(id);
    }

    @Transactional
    public Subject create(SubjectRequest request) {
        if (subjectMapper.findByName(request.name()) != null) {
            throw new ConflictException("SUBJECT_NAME_DUPLICATED", "已存在同名科目");
        }
        Subject subject = new Subject();
        apply(subject, request);
        subjectMapper.insert(subject);
        return subjectMapper.findById(subject.getId());
    }

    @Transactional
    public Subject update(Long id, SubjectRequest request) {
        Subject existing = get(id);
        Subject sameName = subjectMapper.findByName(request.name());
        if (sameName != null && !sameName.getId().equals(existing.getId())) {
            throw new ConflictException("SUBJECT_NAME_DUPLICATED", "已存在同名科目");
        }
        apply(existing, request);
        subjectMapper.update(existing);
        return subjectMapper.findById(id);
    }

    /**
     * 删除科目。已有业务数据引用时拒绝删除：任务单位与科目是历史记录的一部分，
     * 直接删除会让既有学习记录失去归属。
     */
    @Transactional
    public void delete(Long id) {
        get(id);
        if (subjectMapper.countUsages(id) > 0) {
            throw new ConflictException("SUBJECT_IN_USE", "该科目已被学习数据使用，不能删除；可改为停用");
        }
        try {
            subjectMapper.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            // 引用可能在上面的检查之后并发创建，外键仍作为最后一道保护。
            throw new ConflictException("SUBJECT_IN_USE", "该科目已被学习数据使用，不能删除；可改为停用");
        }
    }

    private void apply(Subject subject, SubjectRequest request) {
        subject.setName(request.name().trim());
        subject.setCategory(request.category());
        subject.setColor(request.color());
        subject.setSortOrder(request.sortOrderOrDefault());
        subject.setEnabled(request.enabledOrDefault());
    }
}
