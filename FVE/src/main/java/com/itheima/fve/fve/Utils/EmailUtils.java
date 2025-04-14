package com.itheima.fve.fve.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 邮件发送工具类
 */
@Component
public class EmailUtils {

	private static final Logger log = LoggerFactory.getLogger(EmailUtils.class);

	@Autowired
	private JavaMailSender mailSender; // 注入 Spring Boot 配置好的邮件发送器

	@Value("${spring.mail.username}") // 从配置文件读取发件人邮箱地址
	private String fromEmailAddress;

	/**
	 * 发送简单的文本邮件
	 * @param to 收件人邮箱地址
	 * @param subject 邮件主题
	 * @param text 邮件正文 (可以包含验证码)
	 * @return true 如果发送成功 (注意: 邮件发送是异步的，这里只是表示尝试发送成功)
	 */
	public boolean sendSimpleMail(String to, String subject, String text) {
		if (to == null || to.isEmpty()) {
			log.error("邮件接收者地址不能为空");
			return false;
		}
		if (fromEmailAddress == null || fromEmailAddress.isEmpty()){
			log.error("发件人地址未配置 (spring.mail.username)");
			return false;
		}

		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(fromEmailAddress); // 设置发件人
		message.setTo(to);                // 设置收件人
		message.setSubject(subject);      // 设置主题
		message.setText(text);            // 设置正文

		try {
			log.info("准备发送邮件至: {}, 主题: {}", to, subject);
			mailSender.send(message);
			log.info("邮件已成功发送至: {}", to);
			return true;
		} catch (MailException e) {
			// 捕获邮件发送过程中的异常 (如认证失败、连接超时等)
			log.error("发送邮件至 {} 失败: {}", to, e.getMessage(), e);
			return false;
		} catch (Exception e) {
			// 捕获其他可能的运行时异常
			log.error("发送邮件过程中发生意外错误: {}", e.getMessage(), e);
			return false;
		}
	}
}