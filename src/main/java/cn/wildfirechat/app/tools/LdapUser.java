package cn.wildfirechat.app.tools;

public class LdapUser {
    public final String uid, cn, mail, phone, dn;
    public LdapUser(String uid, String cn, String mail, String phone, String dn) {
        this.uid = uid; this.cn = cn; this.mail = mail; this.phone = phone; this.dn = dn;
    }
    @Override public String toString() {
        return String.format("User{uid='%s', cn='%s', mail='%s', phone='%s', dn='%s'}", uid, cn, mail, phone, dn);
    }
}
