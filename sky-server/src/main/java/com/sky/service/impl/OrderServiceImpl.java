package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;


    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //1、处理异常情况（购物车，地址为空）
        //判断地址信息
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook addressBook = addressBookMapper.getById(addressBookId);
        if(addressBook==null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //判断购物车信息
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart=new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if(shoppingCartList==null || shoppingCartList.size()<0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //2、向订单表插入一条数据
        Orders orders=new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);

        orders.setOrderTime(LocalDateTime.now());
        orders.setUserId(userId);
        orders.setConsignee(addressBook.getConsignee());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));//设置订单编号
        orders.setPhone(addressBook.getPhone());
        orders.setPayStatus(Orders.UN_PAID);//设置支付状态
        orders.setStatus(Orders.PENDING_PAYMENT);//设置订单状态
        orders.setAddress(addressBook.getDetail());

        orderMapper.insert(orders);

        //3、向订单明细表插入n条数据
        List<OrderDetail> orderDetailList=new ArrayList<>();
        Long ordersId = orders.getId();
        for (ShoppingCart cart:shoppingCartList){
            OrderDetail orderDetail=new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(ordersId);
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        //4、删除购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        //5、封装VO对象返回
        OrderSubmitVO orderSubmitVO=OrderSubmitVO.builder()
                .orderNumber(orders.getNumber())//订单号
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = null;
//        try {
//            jsonObject = weChatPayUtil.pay(
//                    ordersPaymentDTO.getOrderNumber(), //商户订单号
//                    new BigDecimal(0.01), //支付金额，单位 元
//                    "苍穹外卖订单", //商品描述
//                    user.getOpenid() //微信用户的openid
//            );
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }

        JSONObject jsonObject=new JSONObject();
        jsonObject.put("code","ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        //支付成功 修改订单状态 及 支付状态 结账时间
        paySuccess(ordersPaymentDTO.getOrderNumber());
//        Orders order=Orders.builder()
//                .number(ordersPaymentDTO.getOrderNumber())
//                .checkoutTime(LocalDateTime.now())//结账时间
//                        .payStatus(Orders.PAID)//支付状态
//                                .status(Orders.TO_BE_CONFIRMED)//订单状态
//                        .build();
//        orderMapper.updateStatus(order);
        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单

        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //通过websocket向客户端推送消息 type  orderId  content
        Map map=new HashMap();
        map.put("type",1);//1 表示来单提醒 2表示客户催单
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号："+outTradeNo);

        String jsonString = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
    }

    @Override
    public PageResult pageQueryForUser(Integer pageNum, Integer pageSize, Integer status) {
        Long userId = BaseContext.getCurrentId();
        PageHelper.startPage(pageNum, pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO=new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(userId);
        ordersPageQueryDTO.setStatus(status);
        //根据筛选条件查询出订单表
        Page<Orders> page=orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list=new ArrayList<>();
        if(page!=null&&page.size()>0){
            for(Orders orders:page){
                //查询每个订单的详细信息
                List<OrderDetail> orderDetailList=orderDetailMapper.getByorderId(orders.getId());

                //封装订单ordersVO
                OrderVO orderVO=new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    @Override
    public OrderVO getByOrderId(Long id) {
        Orders orders=orderMapper.getById(id);
        OrderVO orderVO=new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);//进行类型转换？
        List<OrderDetail> orderDetailList = orderDetailMapper.getByorderId(id);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    @Override
    public void cancelById(Long id) {
        Orders order = orderMapper.getById(id);
        if(order==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消 7退款
        if(order.getStatus()>2){//接单 派送 完成 取消 退款的订单不能取消
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders od=new Orders();
        od.setId(order.getId());
        //待接单的订单需要退款
        if(order.getStatus()==Orders.TO_BE_CONFIRMED){
            //修改支付状态
            od.setPayStatus(Orders.REFUND);
        }
        //修改订单状态为
        od.setStatus(Orders.CANCELLED);
        od.setCancelTime(LocalDateTime.now());
        od.setCancelReason("用户取消");
        orderMapper.update(od);
    }

    @Override
    public void repetitionById(Long id) {
        //将订单详情转换为购物车列表
        Long userId = BaseContext.getCurrentId();
        List<OrderDetail> detailList = orderDetailMapper.getByorderId(id);
        List<ShoppingCart> shoppingCartList=new ArrayList<>();
        for (OrderDetail orderDetail:detailList){
            ShoppingCart cart=new ShoppingCart();
            BeanUtils.copyProperties(orderDetail,cart,"id");//复制属性时忽略id
            cart.setUserId(userId);
            cart.setCreateTime(LocalDateTime.now());
            shoppingCartList.add(cart);
        }
        //批量插入购物车
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    @Override
    public PageResult pageQueryForAdmin(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> orders = orderMapper.pageQuery(ordersPageQueryDTO);
        return new PageResult(orders.getTotal(),orders.getResult());
    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        Long orderId = ordersCancelDTO.getId();
        Orders order = orderMapper.getById(orderId);

        //如果用户已付款 需要退款
        if(order.getPayStatus()==Orders.PAID){
            log.info("申请退款：{}",orderId);
        }
        Orders orders = Orders.builder()
                .id(orderId)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .status(Orders.CANCELLED)
                .build();
        orderMapper.update(orders);

    }

    @Override
    public OrderStatisticsVO statistics() {
        Integer toBeConfirmed=orderMapper.statistics(Orders.TO_BE_CONFIRMED);
        Integer confirmed= orderMapper.statistics(Orders.CONFIRMED);
        Integer deliveryInProgress= orderMapper.statistics(Orders.DELIVERY_IN_PROGRESS);
        OrderStatisticsVO orderStatisticsVO=new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    @Override
    public void reject(OrdersRejectionDTO ordersRejectionDTO) {
        Long orderId = ordersRejectionDTO.getId();
        Orders orders = orderMapper.getById(orderId);

        //只有订单存在且订单状态为待接单状态才能接单
        if(orders==null || orders.getStatus()!=Orders.TO_BE_CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //如果用户已经支付需要退款
        if(orders.getPayStatus()==Orders.PAID){
            log.info("订单退款：{}",orderId);
        }
        Orders order = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .status(Orders.CANCELLED)
                .build();
        orderMapper.update(order);

    }

    @Override
    public void delivery(Long id) {
        Orders orders = orderMapper.getById(id);
        if(orders==null || orders.getStatus()!=Orders.CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders order = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .deliveryTime(LocalDateTime.now()).build();
        orderMapper.update(order);
    }

    @Override
    public void complete(Long id) {
        Orders orders = orderMapper.getById(id);
        if(orders==null || orders.getStatus()!=Orders.DELIVERY_IN_PROGRESS){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders order = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())//送达时间
                .build();
        orderMapper.update(order);
    }

    @Override
    public void remind(Long id) {
        Orders orders = orderMapper.getById(id);
        if(orders==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Map map=new HashMap();
        map.put("type",2);//1表示来单提醒 2表示用户催单
        map.put("orderId",id);
        map.put("content","订单号："+orders.getNumber());

        webSocketServer.sendToAllClient(JSON.toJSONString(map));

    }

}
