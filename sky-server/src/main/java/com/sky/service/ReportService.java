package com.sky.service;

import com.sky.vo.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;

public interface ReportService {

    /**
     * 营业额统计
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);

    /**
     * 用户统计
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    /**
     * 订单统计
     */
    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);

    /**
     * 销量排名前10
     */
    SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end);
    /**
     * 导出业务数据
     */
    void exportBusinessData(HttpServletResponse response) throws IOException;

}
