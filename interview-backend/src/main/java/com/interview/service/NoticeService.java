package com.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.dto.Requests;
import com.interview.entity.Notice;
import com.interview.enums.AdminLogAction;
import com.interview.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private final AdminLogService adminLogService;

    /** 前台公开：仅已启用的公告，按 sort_order 升序。 */
    public List<Notice> listPublic() {
        return noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, 1)
                .orderByAsc(Notice::getSortOrder)
                .orderByAsc(Notice::getId));
    }

    /** 后台：全部公告（含停用），按 sort_order 升序。 */
    public List<Notice> adminList() {
        return noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .orderByAsc(Notice::getSortOrder)
                .orderByAsc(Notice::getId));
    }

    @Transactional
    public Notice create(Requests.NoticeSaveDTO dto) {
        Notice notice = new Notice();
        notice.setContent(dto.getContent().trim());
        notice.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        notice.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        noticeMapper.insert(notice);
        adminLogService.write(AdminLogAction.NOTICE_CREATE, notice.getId(), "新增公告：" + notice.getContent());
        return notice;
    }

    @Transactional
    public Notice update(Long id, Requests.NoticeSaveDTO dto) {
        Notice notice = get(id);
        if (StringUtils.hasText(dto.getContent())) {
            notice.setContent(dto.getContent().trim());
        }
        if (dto.getSortOrder() != null) {
            notice.setSortOrder(dto.getSortOrder());
        }
        if (dto.getStatus() != null) {
            notice.setStatus(dto.getStatus());
        }
        noticeMapper.updateById(notice);
        adminLogService.write(AdminLogAction.NOTICE_UPDATE, id, "编辑公告：" + notice.getContent());
        return notice;
    }

    @Transactional
    public void delete(Long id) {
        Notice notice = get(id);
        noticeMapper.deleteById(id);
        adminLogService.write(AdminLogAction.NOTICE_DELETE, id, "删除公告：" + notice.getContent());
    }

    private Notice get(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        return notice;
    }
}
