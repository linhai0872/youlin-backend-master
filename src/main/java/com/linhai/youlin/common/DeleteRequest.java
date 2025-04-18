package com.linhai.youlin.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用删除请求
 * @author <a href="https://github.com/linhai0872">林海</a>
 */
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = -5860707094194210842L;

    private long id;
}
