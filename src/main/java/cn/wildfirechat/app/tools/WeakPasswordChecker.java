package cn.wildfirechat.app.tools;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 弱口令检验组件
 * <p>
 * 通过配置文件控制检验规则：
 * <ul>
 *   <li>password.weak_check.enable          — 是否开启弱口令校验（默认 true）</li>
 *   <li>password.weak_check.min_length      — 最小密码长度（默认 8）</li>
 *   <li>password.weak_check.require_uppercase — 是否要求大写字母（默认 false）</li>
 *   <li>password.weak_check.require_lowercase — 是否要求小写字母（默认 false）</li>
 *   <li>password.weak_check.require_digit   — 是否要求数字（默认 false）</li>
 *   <li>password.weak_check.require_special_char — 是否要求特殊字符（默认 false）</li>
 *   <li>password.weak_check.forbidden_list  — 禁止使用的常见弱口令，逗号分隔</li>
 * </ul>
 */
@Component
public class WeakPasswordChecker {

    @Value("${password.weak_check.enable:true}")
    private boolean enable;

    @Value("${password.weak_check.min_length:8}")
    private int minLength;

    @Value("${password.weak_check.require_uppercase:false}")
    private boolean requireUppercase;

    @Value("${password.weak_check.require_lowercase:false}")
    private boolean requireLowercase;

    @Value("${password.weak_check.require_digit:false}")
    private boolean requireDigit;

    @Value("${password.weak_check.require_special_char:false}")
    private boolean requireSpecialChar;

    /**
     * 逗号分隔的禁止密码列表，例如：
     * password.weak_check.forbidden_list=123456,password,111111,qwerty,abc123
     */
    @Value("${password.weak_check.forbidden_list:123456,password,123456789,12345678,111111,qwerty,abc123,123123,000000,iloveyou,admin,letmein,welcome,monkey,dragon}")
    private String forbiddenListStr;

    private Set<String> forbiddenSet;

    @javax.annotation.PostConstruct
    private void init() {
        forbiddenSet = new HashSet<>();
        if (forbiddenListStr != null && !forbiddenListStr.isEmpty()) {
            Arrays.stream(forbiddenListStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(forbiddenSet::add);
        }
    }

    /**
     * 检查密码是否为弱口令。
     *
     * @param password 待检查的明文密码
     * @return {@code true} 表示是弱口令（不符合要求），{@code false} 表示密码合格
     */
    public boolean isWeakPassword(String password) {
        if (!enable) {
            return false;
        }

        if (password == null || password.length() < minLength) {
            return true;
        }

        if (requireUppercase && password.chars().noneMatch(Character::isUpperCase)) {
            return true;
        }

        if (requireLowercase && password.chars().noneMatch(Character::isLowerCase)) {
            return true;
        }

        if (requireDigit && password.chars().noneMatch(Character::isDigit)) {
            return true;
        }

        if (requireSpecialChar && password.chars().allMatch(c -> Character.isLetterOrDigit(c) || Character.isWhitespace(c))) {
            return true;
        }

        if (forbiddenSet != null && forbiddenSet.contains(password)) {
            return true;
        }

        return false;
    }
}
