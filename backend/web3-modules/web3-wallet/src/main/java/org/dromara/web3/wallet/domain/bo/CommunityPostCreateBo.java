package org.dromara.web3.wallet.domain.bo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommunityPostCreateBo {

    private String title;

    private String content;

    private List<String> imageList = new ArrayList<>();
}
