package com.itheima.fve.fve.dto;

import com.itheima.fve.fve.entity.OrderDetail;
import com.itheima.fve.fve.entity.Orders;
import lombok.Data;
import java.util.List;

@Data
public class OrdersDto extends Orders {

    private String userName;

    private String phone;

    private String address;

    private String consignee;

    private List<OrderDetail> orderDetails;
	
}
