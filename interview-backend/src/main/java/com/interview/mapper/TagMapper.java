package com.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.entity.Tag;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

public interface TagMapper extends BaseMapper<Tag> {

    /**
     * 按名称批量查询标签（IN 查询）。
     */
    @Select("<script>" +
            "SELECT * FROM tag WHERE name IN " +
            "<foreach collection='names' item='name' open='(' separator=',' close=')'>#{name}</foreach>" +
            "</script>")
    List<Tag> selectByNames(@Param("names") Collection<String> names);

    /**
     * 批量新增标签（多行 VALUES）。
     */
    @Insert("<script>" +
            "INSERT INTO tag (name) VALUES " +
            "<foreach collection='list' item='t' separator=','>(#{t.name})</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<Tag> tags);
}
