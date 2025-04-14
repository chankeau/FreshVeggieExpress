package com.itheima.fve.fve.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.fve.fve.entity.AddressBook;
import com.itheima.fve.fve.mapper.AddressBookMapper;
import com.itheima.fve.fve.service.AddressBookService;
import org.springframework.stereotype.Service;

/**
 * @Author 王开朗
 * @Date 2022/6/1 10:19
 * @Description
 * @Vesion
 **/
@Service
public class AddressBookServiceImpl extends ServiceImpl<AddressBookMapper, AddressBook> implements AddressBookService {
}
