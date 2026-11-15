package io.changyeoga.app

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AppFont=FontFamily(Font(R.font.pretendard_regular,FontWeight.Normal),Font(R.font.pretendard_semibold,FontWeight.SemiBold))
fun appTypography():Typography {
    val base=Typography()
    fun style(size:Int,line:Int,weight:FontWeight=FontWeight.Normal)=TextStyle(fontFamily=AppFont,fontSize=size.sp,lineHeight=line.sp,fontWeight=weight)
    return base.copy(
        displayLarge=base.displayLarge.copy(fontFamily=AppFont),displayMedium=base.displayMedium.copy(fontFamily=AppFont),displaySmall=base.displaySmall.copy(fontFamily=AppFont),
        headlineLarge=style(30,40,FontWeight.SemiBold),headlineMedium=style(26,34,FontWeight.SemiBold),headlineSmall=style(24,32,FontWeight.SemiBold),
        titleLarge=style(20,28,FontWeight.SemiBold),titleMedium=style(17,24,FontWeight.SemiBold),titleSmall=style(14,20,FontWeight.SemiBold),
        bodyLarge=style(16,24),bodyMedium=style(14,20),bodySmall=style(13,18),
        labelLarge=style(16,22,FontWeight.SemiBold),labelMedium=style(13,18,FontWeight.SemiBold),labelSmall=style(12,16))
}
@Composable
fun AppSymbol(resource:Int,description:String?=null) {
    Icon(painterResource(resource),description,Modifier.size(if(BuildConfig.IS_VEHICLE) 32.dp else 24.dp))
}
