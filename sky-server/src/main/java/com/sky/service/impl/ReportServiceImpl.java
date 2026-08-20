package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.dto.GoodsSalesDTO;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {


    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;
    /**
     * 营业额统计
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
//当前集合用于存放从begin到end所有的日期
        List<LocalDate> dateList = new ArrayList<>();
        do {
            dateList.add(begin);
            begin = begin.plusDays(1);
        } while (!begin.isAfter(end));

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList){
        //查询date的营业额,
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            if (turnover == null) {
                turnover = 0.0;
            }
            turnoverList.add(turnover);
        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build() ;
    }


    /**
     * 用户统计
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        //当前集合用于存放从begin到end所有的日期
        dateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //存放每天新增的用户数量
        List<Integer> newUserList = new ArrayList<>();
        //存放每天总用户数量
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();


            map.put("end", endTime);
            Integer totalUserCount = userMapper.countByMap(map );

            map.put("begin", beginTime);
            Integer newUserCount = userMapper.countByMap(map);
            totalUserList.add(totalUserCount);
            newUserList.add(newUserCount);
        }

        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build() ;

    }

    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        for(LocalDate date : dateList)
        {
            //查询每天订单总数
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer orderCount = getOrderCount(beginTime, endTime, null);
            //查询有效的订单数
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);

        }
        //计算每天的订单总数
        Integer totalOrderCount = orderCountList.stream().mapToInt(Integer::intValue).sum();
        //计算每天有效的订单数
        Integer totalValidOrderCount = validOrderCountList.stream().mapToInt(Integer::intValue).sum();
        //计算订单完成率
        Double orderCompletionRate =0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = (double) totalValidOrderCount / totalOrderCount;
        }

        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(totalValidOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();

    }
    //根据条件查询订单数量
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map map = new HashMap();
        map.put("begin", beginTime);
        map.put("end", endTime);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }

    /**
     * 销量排名前10
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getSalesTop10(beginTime, endTime);

        List<String> names = goodsSalesDTOList.stream()
                .map(GoodsSalesDTO::getName)
                .collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");

        List<Integer> numbers = goodsSalesDTOList.stream()
                .map(GoodsSalesDTO::getNumber)
                .collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 导出运营数据
     */

    public void exportBusinessData(HttpServletResponse response) throws IOException {
        //查询数据库
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        BusinessDataVO businessDataVO = workspaceService.getBusinessData(
                LocalDateTime.of(dateBegin, LocalTime.MIN),
                LocalDateTime.of(dateEnd, LocalTime.MAX));
        long days = ChronoUnit.DAYS.between(dateBegin, dateEnd);
        //读取Excel模板文件
        InputStream in = this.getClass()
                .getClassLoader()
                .getResourceAsStream("/templates/运营数据报表模板.xlsx");

        //基于模板创建Excel并填充数据
        XSSFWorkbook excel = new XSSFWorkbook(in);
        XSSFSheet sheet = excel.getSheet("Sheet1");
        sheet.getRow(1).getCell(1).setCellValue("时间:" + dateBegin + "至" + dateEnd);

        XSSFRow row = sheet.getRow(3);
        row.getCell(2).setCellValue(businessDataVO.getTurnover());
        row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
        row.getCell(6).setCellValue(businessDataVO.getNewUsers());

        XSSFRow row2 = sheet.getRow(4);
        row2.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
        row2.getCell(4).setCellValue(businessDataVO.getUnitPrice());
        //填充明细数据
        for (int i = 0; i <= days; i++) {
            LocalDate date = dateBegin.plusDays(i);
            BusinessDataVO businessData = workspaceService.getBusinessData(
                    LocalDateTime.of(date, LocalTime.MIN),
                    LocalDateTime.of(date, LocalTime.MAX));
            row = sheet.getRow(i + 7);
            row.getCell(1).setCellValue(date.toString());
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(3).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(5).setCellValue(businessData.getUnitPrice());
            row.getCell(6).setCellValue(businessData.getNewUsers());
        }

        //将Excel写入到客户端
        ServletOutputStream out = response.getOutputStream();
        excel.write(out);
        excel.close();
        out.close();
    }
}
