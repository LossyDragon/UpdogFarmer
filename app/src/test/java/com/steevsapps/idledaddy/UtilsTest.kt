package com.steevsapps.idledaddy

import com.steevsapps.idledaddy.utils.Utils.removeSpecialChars
import org.junit.Assert
import org.junit.Test

class UtilsTest {
    /**
     * Test the removeSpecialChars method
     */
    @Test
    fun removeSpecialChars_works() {
        Assert.assertEquals("daddy123", removeSpecialChars("daಥd益dಥy123"))
    }
}
