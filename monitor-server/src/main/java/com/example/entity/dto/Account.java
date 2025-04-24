package com.example.entity.dto;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.entity.BaseData;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 数据库中的用户信息
 */
@Data
@TableName("db_account")
@AllArgsConstructor
public class Account implements BaseData {
    @TableId(type = IdType.AUTO)
    Integer id;
    String username;
    String password;
    String email;
    String role;
    Date registerTime;
    String clients;

    public List<Integer> getClientList() {
        if(clients == null) return Collections.emptyList();
        return JSONArray.parse(clients).toList(Integer.class);  //将string对象转换为json的格式  再转换为以List<Integer>存放
                                                                //先将（string）[50663414, 33408106] 转换为"[50663414, 33408106]," 最后list化
    }
}
