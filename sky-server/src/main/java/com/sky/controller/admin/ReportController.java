package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.io.IOException;

/**
 * 数据统计相关
 */
@RestController
@RequestMapping("/admin/report")
@Slf4j
@Api(tags = "数据统计相关接口")
public class ReportController {

    @Autowired
    private ReportService reportService;
    /**
     * 营业额统计
     */
    @GetMapping("/turnoverStatistics")
    @ApiOperation(value = "营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("营业额统计，开始时间：{}，结束时间：{}", begin, end);
        return Result.success( reportService.getTurnoverStatistics(begin, end));
    }


    /**
     * 用户统计
     */
    @GetMapping("/userStatistics")
    @ApiOperation(value = "用户统计")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("用户统计，开始时间：{}，结束时间：{}", begin, end);
        return Result.success(reportService.getUserStatistics(begin, end));

    }
    /**
     * 订单统计
     */
    @GetMapping("/orderStatistics")
    @ApiOperation(value = "订单统计")
    public Result<OrderReportVO> orderStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("订单统计，开始时间：{}，结束时间：{}", begin, end);
        return Result.success(reportService.getOrderStatistics(begin, end));
    }
    /**
     * 销量排名
     */
    @GetMapping("/top10")
    @ApiOperation(value = "销量排名前10")
    public Result<SalesTop10ReportVO> top10(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("销量排名前10，开始时间：{}，结束时间：{}", begin, end);
        return Result.success(reportService.getSalesTop10(begin, end));
    }
    /**
     * 导出运营数据excle表
     */

    @GetMapping("/export")
    @ApiOperation("导出运营数据excle表")
    public void export(HttpServletResponse response) throws IOException {

        reportService.exportBusinessData(response);
    }
}
