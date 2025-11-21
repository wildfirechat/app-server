package cn.wildfirechat.app.tools;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Hashtable;
import java.util.List;

public class LdapUtil {

    /** 根据电话号码反向查人 */
    public static List<LdapUser> findUserByPhone(String phone, String ldapUrl, String searchBase, String adminDn, String adminPwd) throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapUrl);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, adminDn);
        env.put(Context.SECURITY_CREDENTIALS, adminPwd);

        DirContext ctx = new InitialDirContext(env);
        try {
            /* 搜索过滤器：电话号码完全匹配 */
            String filter = "(&(objectClass=inetOrgPerson)(telephoneNumber={0}))";
            Object[] params = { phone };

            SearchControls ctls = new SearchControls();
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            /* 只取我们关心的属性 */
            ctls.setReturningAttributes(new String[] { "uid", "cn", "mail", "telephoneNumber" });

            NamingEnumeration<SearchResult> rs = ctx.search(searchBase, filter, params, ctls);
            List<LdapUser> ldapUsers = new ArrayList<>();
            while (rs.hasMore()) {
                SearchResult sr = rs.next();
                Attributes attrs = sr.getAttributes();
                String dn = sr.getNameInNamespace();
                ldapUsers.add(new LdapUser(
                        getAttr(attrs, "uid"),
                        getAttr(attrs, "cn"),
                        getAttr(attrs, "mail"),
                        getAttr(attrs, "telephoneNumber"),
                        dn));
            }
            return ldapUsers;
        } finally {
            ctx.close();
        }
    }

    /**
     * OpenLDAP 密码加密：SSHA（Salted SHA-1，带随机盐值，更安全）
     * @param password 明文密码
     * @return Base64 编码后的 SSHA 密码（LDAP 存储格式）
     */
    private static String encodeSshaPassword(String password) {
        try {
            // 1. 生成 8 字节随机盐值（增加破解难度）
            byte[] salt = new byte[8];
            new java.security.SecureRandom().nextBytes(salt);

            // 2. 密码字节 + 盐值字节
            byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
            byte[] passwordWithSalt = new byte[passwordBytes.length + salt.length];
            System.arraycopy(passwordBytes, 0, passwordWithSalt, 0, passwordBytes.length);
            System.arraycopy(salt, 0, passwordWithSalt, passwordBytes.length, salt.length);

            // 3. SHA-1 哈希（可替换为 SHA-256，需 LDAP 服务器支持）
            byte[] shaHash = java.security.MessageDigest.getInstance("SHA-1").digest(passwordWithSalt);

            // 4. SSHA 格式：{SSHA} + Base64(哈希值 + 盐值)
            byte[] sshaBytes = new byte[shaHash.length + salt.length];
            System.arraycopy(shaHash, 0, sshaBytes, 0, shaHash.length);
            System.arraycopy(salt, 0, sshaBytes, shaHash.length, salt.length);

            return "{SSHA}" + Base64.getEncoder().encodeToString(sshaBytes);
        } catch (Exception e) {
            throw new RuntimeException("SSHA 密码加密失败", e);
        }
    }
    private static String getAttr(Attributes attrs, String name) throws NamingException {
        Attribute attr = attrs.get(name);
        return attr == null ? null : (String) attr.get();
    }
}
