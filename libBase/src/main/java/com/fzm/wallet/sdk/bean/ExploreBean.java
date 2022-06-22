package com.fzm.wallet.sdk.bean;

import java.util.ArrayList;
public class ExploreBean {

    /**
     * id : 1
     * name : 热门应用
     * style : 2
     * apps : [{"id":1,"name":"红包","icon":"https://service.biqianbao.net/upload/coin/2018102215083121087196435.jpg","type":1,"app_url":"https://www.zhengfan.com","slogan":"这是红包"},{"id":2,"name":"找币","icon":"https://service.biqianbao.net/upload/coin/2019062719421156978110432.png","type":1,"app_url":"https://www.fan.com","slogan":"这是找币"},{"id":3,"name":"云矿机","icon":"https://service.biqianbao.net/upload/coin/2019062719421156978110432.png","type":2,"app_url":"https://www.fana.com","slogan":"这是云矿机"}]
     */

    private int id;
    private String name;
    private int style;
    private ArrayList<AppsBean> apps;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStyle() {
        return style;
    }

    public void setStyle(int style) {
        this.style = style;
    }

    public ArrayList<AppsBean> getApps() {
        return apps;
    }

    public void setApps(ArrayList<AppsBean> apps) {
        this.apps = apps;
    }

    public static class AppsBean {
        /**
         * id : 1
         * name : 红包
         * icon : https://service.biqianbao.net/upload/coin/2018102215083121087196435.jpg
         * type : 1
         * app_url : https://www.zhengfan.com
         * slogan : 这是红包
         */

        private int id;
        private String name;
        private String icon;
        private int type;
        private String app_url;
        private String slogan;
        private int email;
        private int phone;
        private int real_name;

        public int getEmail() {
            return email;
        }

        public void setEmail(int email) {
            this.email = email;
        }

        public int getPhone() {
            return phone;
        }

        public void setPhone(int phone) {
            this.phone = phone;
        }

        public int getReal_name() {
            return real_name;
        }

        public void setReal_name(int real_name) {
            this.real_name = real_name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getApp_url() {
            return app_url;
        }

        public void setApp_url(String app_url) {
            this.app_url = app_url;
        }

        public String getSlogan() {
            return slogan;
        }

        public void setSlogan(String slogan) {
            this.slogan = slogan;
        }
    }
}
