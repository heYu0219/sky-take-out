package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import io.swagger.models.auth.In;
import net.bytebuddy.asm.Advice;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.codehaus.jettison.util.FastStack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //存放日期列表
        List<LocalDate> dateList=new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double> turnoverList=new ArrayList<>();
        for (LocalDate date:dateList){
            //查询date日期对应的营业额数据
            LocalDateTime beginTime=LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime=LocalDateTime.of(date, LocalTime.MAX);

            Map map=new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover=orderMapper.sumByMap(map);
            if(turnover==null){
                turnover=0.0;
            }
            turnoverList.add(turnover);
        }
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //存放日期列表
        List<LocalDate> dateList=new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> totalUsers=new ArrayList<>();
        List<Integer> newUsers=new ArrayList<>();
        for(LocalDate date:dateList){
            LocalDateTime beginTime=LocalDateTime.of(date,LocalTime.MIN);
            LocalDateTime endTime=LocalDateTime.of(date,LocalTime.MAX);
            Map map=new HashMap();
            map.put("begin",beginTime);
            Integer totaluser=userMapper.getUserCount(map);
            map.put("end",endTime);
            Integer newUser=userMapper.getUserCount(map);
            totalUsers.add(totaluser);
            newUsers.add(newUser);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(newUsers,","))
                .totalUserList(StringUtils.join(totalUsers,","))
                .build();
    }

    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        //存放日期列表
        List<LocalDate> dateList=new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> totalOrderList=new ArrayList<>();
        List<Integer> validOrderList=new ArrayList<>();

        for(LocalDate date:dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Integer totalOrder = getOrderCount(beginTime, endTime, null);
            Integer validOrder = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            totalOrderList.add(totalOrder);
            validOrderList.add(validOrder);
        }
        //统计订单总量 和 有效订单数
        Integer totalCount = totalOrderList.stream().reduce(Integer::sum).get();
        Integer validCount = validOrderList.stream().reduce(Integer::sum).get();

        Double orderCompletionRate=0.0;
        if(totalCount!=0){
            orderCompletionRate= validCount.doubleValue()/totalCount;
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(totalOrderList,","))
                .totalOrderCount(totalCount)
                .validOrderCountList(StringUtils.join(validOrderList,","))
                .validOrderCount(validCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime=LocalDateTime.of(begin,LocalTime.MIN);
        LocalDateTime endTime=LocalDateTime.of(end,LocalTime.MAX);
        List<GoodsSalesDTO> salesTop10=orderMapper.getTop10(beginTime,endTime);

        //使用流对象将GoodsSalesDTO中的name提取出来成为一个集合
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO
                .builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    private Integer getOrderCount(LocalDateTime beginTime,LocalDateTime endTime,Integer status){
        Map map=new HashMap();
        map.put("begin",beginTime);
        map.put("end",endTime);
        map.put("status",status);
        return orderMapper.countByMap(map);
    }
}
