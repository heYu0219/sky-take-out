package com.sky.service;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);

    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO);

    void paySuccess(String outTradeNo);

    PageResult pageQueryForUser(Integer page, Integer pageSize, Integer status);

    OrderVO getByOrderId(Long id);

    void cancelById(Long id);

    void repetitionById(Long id);
}
