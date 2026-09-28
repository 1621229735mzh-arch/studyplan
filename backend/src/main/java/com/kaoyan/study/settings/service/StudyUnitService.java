package com.kaoyan.study.settings.service;

import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.common.exception.NotFoundException;
import com.kaoyan.study.settings.dto.StudyUnitRequest;
import com.kaoyan.study.settings.entity.StudyUnit;
import com.kaoyan.study.settings.mapper.StudyUnitMapper;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 计量单位用例。 */
@Service
public class StudyUnitService {

    private final StudyUnitMapper studyUnitMapper;

    public StudyUnitService(StudyUnitMapper studyUnitMapper) {
        this.studyUnitMapper = studyUnitMapper;
    }

    public List<StudyUnit> list(boolean enabledOnly) {
        return studyUnitMapper.findAll(enabledOnly);
    }

    public StudyUnit get(Long id) {
        StudyUnit unit = studyUnitMapper.findById(id);
        if (unit == null) {
            throw new NotFoundException("计量单位不存在");
        }
        return unit;
    }

    @Transactional
    public StudyUnit create(StudyUnitRequest request) {
        if (studyUnitMapper.findByName(request.name()) != null) {
            throw new ConflictException("UNIT_NAME_DUPLICATED", "已存在同名计量单位");
        }
        StudyUnit unit = new StudyUnit();
        apply(unit, request);
        studyUnitMapper.insert(unit);
        return studyUnitMapper.findById(unit.getId());
    }

    @Transactional
    public StudyUnit update(Long id, StudyUnitRequest request) {
        StudyUnit existing = get(id);
        StudyUnit sameName = studyUnitMapper.findByName(request.name());
        if (sameName != null && !sameName.getId().equals(existing.getId())) {
            throw new ConflictException("UNIT_NAME_DUPLICATED", "已存在同名计量单位");
        }
        apply(existing, request);
        studyUnitMapper.update(existing);
        return studyUnitMapper.findById(id);
    }

    /** 删除单位；被任务或学习记录引用时拒绝，避免历史数据失去单位语义。 */
    @Transactional
    public void delete(Long id) {
        get(id);
        if (studyUnitMapper.countUsages(id) > 0) {
            throw new ConflictException("UNIT_IN_USE", "该单位已被任务或学习记录使用，不能删除；可改为停用");
        }
        try {
            studyUnitMapper.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("UNIT_IN_USE", "该单位已被任务或学习记录使用，不能删除；可改为停用");
        }
    }

    private void apply(StudyUnit unit, StudyUnitRequest request) {
        unit.setName(request.name().trim());
        unit.setSortOrder(request.sortOrderOrDefault());
        unit.setEnabled(request.enabledOrDefault());
    }
}
