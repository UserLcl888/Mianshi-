package com.interview.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interview.common.PageResult;
import com.interview.dto.VOs;
import com.interview.entity.AdminLog;
import com.interview.mapper.AdminLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final AdminLogMapper adminLogMapper;

    public void write(String action, String targetType, Long targetId, String detail) {
        AdminLog log = new AdminLog();
        log.setAdminId(StpUtil.getLoginIdAsLong());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail == null ? "" : detail);
        adminLogMapper.insert(log);
    }

    public PageResult<VOs.AdminLogVO> list(long page, long size) {
        Page<AdminLog> result = adminLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<AdminLog>().orderByDesc(AdminLog::getId));
        return PageResult.of(page, size, result.getTotal(),
                result.getRecords().stream().map(this::toVO).toList());
    }

    private VOs.AdminLogVO toVO(AdminLog log) {
        return VOs.AdminLogVO.builder()
                .id(log.getId())
                .adminId(log.getAdminId())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .detail(log.getDetail())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
