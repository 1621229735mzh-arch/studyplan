package com.kaoyan.study.target.mapper;

import com.kaoyan.study.target.entity.TargetLink;
import com.kaoyan.study.target.entity.TargetSchool;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 候选目标数据访问，一个接口覆盖 {@code target_school} 与其从属的 {@code target_link}。
 * SQL 见 {@code resources/mapper/target/TargetMapper.xml}。
 */
@Mapper
public interface TargetMapper {

    /** 按状态过滤；status 为空表示不过滤。 */
    List<TargetSchool> findAll(@Param("status") String status);

    TargetSchool findById(@Param("id") Long id);

    /** 某个目标的参考链接，按 sort_order 排列。 */
    List<TargetLink> findLinksByTargetId(@Param("targetId") Long targetId);

    int insert(TargetSchool target);

    /** 按 id + version 更新，返回受影响行数；0 表示版本已过期。 */
    int update(TargetSchool target);

    /** 只改状态，同样按 id + version 校验；0 表示版本已过期。 */
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("version") Long version);

    int deleteById(@Param("id") Long id);

    int deleteLinksByTargetId(@Param("targetId") Long targetId);

    int insertLink(TargetLink link);
}
