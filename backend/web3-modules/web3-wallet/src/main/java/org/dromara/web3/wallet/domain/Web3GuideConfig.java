package org.dromara.web3.wallet.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("web3_guide_config")
public class Web3GuideConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 语言(zh-CN, zh-TW, en)
     */
    private String language;

    private String name;
    private String eco;
    private String tool;
    private String heroTitle;
    private String heroSubtitle;
    private String heroBtnText;
    private String firstTitle;
    private String firstSubtitle;
    private String secondTitle;
    private String secondSubtitle;

    /**
     * JSON 格式的列表数据
     */
    @TableField("content_list")
    private String contentList;
    @TableField("menu_list")
    private String menuList;
    @TableField("tool_list")
    private String toolList;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

}
