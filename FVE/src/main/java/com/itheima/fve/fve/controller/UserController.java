package com.itheima.fve.fve.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.data.redis.core.RedisTemplate;
import com.itheima.fve.fve.Utils.EmailUtils;
import com.itheima.fve.fve.Utils.ValidateCodeUtils;
import com.itheima.fve.fve.common.R;
import com.itheima.fve.fve.entity.User;
import com.itheima.fve.fve.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.itheima.fve.fve.dto.UserRegisterDto;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {


    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private EmailUtils emailUtils;


    private static final String REDIS_CODE_PREFIX = "login:code:";

    /**
     * 发送邮箱验证码
     *
     * @param user      Frontend should send { "email": "xxx@example.com" }
     * @param httpSession Not strictly needed here if using Redis, but kept for consistency
     * @return R<String> Result message
     */
    @PostMapping("/sendMsg")

    public R<String> sendMsg(@RequestBody Map<String, String> payload) {

        //获取邮箱地址
        // String email = user.getEmail();
        String email = payload.get("email");


        if (StringUtils.isNotEmpty(email)) {

            //生成随机的六位验证码 (Common for email)
            String code = ValidateCodeUtils.generateValidateCode(6).toString();
            log.info("为邮箱 {} 生成的验证码: {}", email, code);

            // 构建邮件内容
            String subject = "【蔬鲜递】登录验证码"; // Your application name
            String text = "尊敬的用户，您的登录验证码是：" + code + "，此验证码5分钟内有效。请勿泄露给他人。";

            // 调用 EmailUtils 发送邮件
            boolean sent = emailUtils.sendSimpleMail(email, subject, text);

            if (sent) {
                // 将生成的验证码保存到 Redis，以 email 为 key，设置5分钟过期时间
                String redisKey = REDIS_CODE_PREFIX + email;
                redisTemplate.opsForValue().set(redisKey, code, 5, TimeUnit.MINUTES);
                log.info("验证码已存入 Redis，Key: {}", redisKey);
                return R.success("验证码邮件已发送，请注意查收");
            } else {
                // Email sending failed (EmailUtils should log the specific error)
                return R.error("验证码邮件发送失败，请稍后重试或检查邮箱地址");
            }
        }
        return R.error("请输入有效的邮箱地址");
    }

    /**
     * 用户邮箱验证码登录
     *
     * @param map     Frontend should send { "email": "xxx@example.com", "code": "123456" }
     * @param session HttpSession to store user login state
     * @return R<User> Logged-in user information or error
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map<String, String> map, HttpSession session) {
        log.debug("接收到登录请求: {}", map); // Use debug level for request details

        //获取邮箱地址
        String email = map.get("email");
        //获取验证码
        String code = map.get("code");


        if (StringUtils.isAnyEmpty(email, code)) {
            return R.error("邮箱地址或验证码不能为空");
        }


        // 从 Redis 中获取缓存的验证码
        String redisKey = REDIS_CODE_PREFIX + email;
        String codeInRedis = redisTemplate.opsForValue().get(redisKey);
        log.debug("从 Redis (Key: {}) 获取到的验证码: {}", redisKey, codeInRedis);

        //进行验证码的比对 (忽略大小写通常更友好)
        if (codeInRedis != null && codeInRedis.equalsIgnoreCase(code)) {
            //如果比对成功，说明登录成功

            //判断当前邮箱对应的用户是否为新用户。如果是新用户就自动完成注册
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

            queryWrapper.eq(User::getEmail, email);
            User user = userService.getOne(queryWrapper);

            if (user == null) {
                // 用户不存在，进行自动注册
                log.info("邮箱 {} 未注册，执行自动注册", email);
                user = new User();

                user.setEmail(email);
                user.setStatus(1);

                try {
                    userService.save(user);
                    log.info("新用户注册成功: {}", user);
                } catch (Exception e) {
                    log.error("自动注册失败 for email {}: {}", email, e.getMessage(), e);
                    return R.error("用户注册失败，请稍后再试");
                }

            } else {
                log.info("用户 {} 登录成功", email);
            }


            // 登录成功，将用户 ID 存入 Session (Standard practice)
            session.setAttribute("user", user.getId());

            //如果验证码校验成功，则删除 Redis 中缓存的验证码
            redisTemplate.delete(redisKey);
            log.debug("已从 Redis 删除验证码，Key: {}", redisKey);


            return R.success(user);
        }

        // 验证码不匹配或已过期
        log.warn("邮箱 {} 的验证码 {} 不正确或已过期 (Redis 中: {})", email, code, codeInRedis);
        return R.error("验证码不正确或已过期");
    }
    /**
     * 处理用户注册请求
     *
     * @param userRegisterDTO 包含注册信息的 DTO (name, phone, email, sex, idNumber, code)
     * @return R<String> 注册结果
     */
    @PostMapping("/register")
    public R<String> register(@Validated @RequestBody UserRegisterDto userRegisterDTO) { // 使用 DTO 接收数据
        log.info("接收到注册请求: {}", userRegisterDTO);

        String email = userRegisterDTO.getEmail();
        String code = userRegisterDTO.getCode();

        // 1. 基础校验 (DTO 验证可以处理大部分)
        if (StringUtils.isAnyEmpty(email, code /*, 其他必填项如 name, phone 等 */)) {
            return R.error("注册信息不完整");
        }

        // 2. 校验验证码 (与登录逻辑类似)
        String redisKey = REDIS_CODE_PREFIX + email;
        String codeInRedis = redisTemplate.opsForValue().get(redisKey);
        log.debug("注册流程 - 从 Redis (Key: {}) 获取到的验证码: {}", redisKey, codeInRedis);

        if (codeInRedis == null || !codeInRedis.equalsIgnoreCase(code)) {
            log.warn("注册流程 - 邮箱 {} 的验证码 {} 不正确或已过期 (Redis 中: {})", email, code, codeInRedis);
            return R.error("验证码不正确或已过期");
        }

        // 3. 检查邮箱是否已注册 (可选，取决于你的业务逻辑)
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, email);
        if (userService.count(queryWrapper) > 0) {
            log.warn("尝试注册已存在的邮箱: {}", email);
            return R.error("该邮箱已被注册");
        }
         LambdaQueryWrapper<User> phoneQuery = new LambdaQueryWrapper<>();
         phoneQuery.eq(User::getPhone, userRegisterDTO.getPhone());
         if (userService.count(phoneQuery) > 0) {
             log.warn("尝试注册已存在的手机号: {}", userRegisterDTO.getPhone());
             return R.error("该手机号已被注册");
         }


        // 4. 调用 Service 层执行注册逻辑
        try {
            User user = new User();
            user.setName(userRegisterDTO.getName());
            user.setPhone(userRegisterDTO.getPhone());
            user.setEmail(userRegisterDTO.getEmail());
            user.setSex(userRegisterDTO.getSex());
            user.setIdNumber(userRegisterDTO.getIdNumber());
            user.setStatus(1);


            userService.save(user);
            log.info("新用户注册成功: {}", user);

            // 注册成功后，删除验证码
            redisTemplate.delete(redisKey);
            log.debug("注册成功，已从 Redis 删除验证码，Key: {}", redisKey);

            return R.success("注册成功！"); // 返回成功消息给前端

        } catch (Exception e) {
            log.error("注册过程中发生错误 for email {}: {}", email, e.getMessage(), e);
            return R.error("注册失败，请稍后再试");
        }
    }
    /**
     * 用户退出登录
     *
     * @param request HttpServletRequest to access session
     * @return R<String> Success message
     */
    @PostMapping("/loginout")
    public R<String> logout(HttpServletRequest request) {
        // 从 Session 中移除用户登录标识
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.info("用户退出登录, Session ID: {}, User ID: {}", session.getId(), session.getAttribute("user"));
            session.removeAttribute("user");

        } else {
            log.info("用户退出登录，但 Session 不存在或已失效");
        }
        return R.success("退出成功");
    }
}