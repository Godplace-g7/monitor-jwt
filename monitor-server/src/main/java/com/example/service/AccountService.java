package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.Account;
import com.example.entity.vo.request.*;
import com.example.entity.vo.response.SubAccountVO;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface AccountService extends IService<Account>, UserDetailsService {
    Account findAccountByNameOrEmail(String text);
    String registerEmailVerifyCode(String type, String email, String address);
    String registerEmailAccount(EmailRegisterVO info);
    String resetEmailAccountPassword(EmailResetVO info);
    String resetConfirm(ConfirmResetVO info);

    //改密码接口实现
    boolean changePassword(int id, String oldPass, String newPass);

    //创建子用户
    void createSubAccount(CreateSubAccountVO vo);

    //删除子用户
    void deleteSubAccount(int uid);

    //查询子用户
    List<SubAccountVO> listSubAccount();

    //更改邮件操作
    String modifyEmail(int id, ModifyEmailVO vo);
}
