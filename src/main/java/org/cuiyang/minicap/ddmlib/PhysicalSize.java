package org.cuiyang.minicap.ddmlib;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * physical size
 *
 * @author cuiyang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhysicalSize {
    /** 宽度 */
    private int width;
    /** 高度 */
    private int height;
}
