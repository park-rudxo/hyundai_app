package io.changyeoga.app

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AppFont=FontFamily(Font(R.font.pretendard_regular,FontWeight.Normal),Font(R.font.pretendard_semibold,FontWeight.SemiBold))
val Canvas = Color(0xFFFAFAF8)
val TextInk = Color(0xFF30383E)
val ActionBlue = Color(0xFF426A92)
val MutedInk = Color(0xFF697278)
val Rule = Color(0xFFE5E7E7)
fun appColors()=lightColorScheme(primary=ActionBlue,onPrimary=Canvas,background=Canvas,
    surface=Canvas,surfaceVariant=Color(0xFFF0F1EF),onSurface=TextInk,onBackground=TextInk,
    onSurfaceVariant=MutedInk,outline=Color(0xFFADB9C2),outlineVariant=Rule,
    secondary=ActionBlue,tertiary=ActionBlue,error=Color(0xFFA13E38))
fun appTypography():Typography {
    val base=Typography()
    fun style(size:Int,line:Int,weight:FontWeight=FontWeight.Normal)=TextStyle(fontFamily=AppFont,fontSize=size.sp,lineHeight=line.sp,fontWeight=weight)
    return base.copy(
        displayLarge=base.displayLarge.copy(fontFamily=AppFont),displayMedium=base.displayMedium.copy(fontFamily=AppFont),displaySmall=base.displaySmall.copy(fontFamily=AppFont),
        headlineLarge=style(30,40),headlineMedium=style(26,36),headlineSmall=style(24,32),
        titleLarge=style(20,28),titleMedium=style(17,26),titleSmall=style(14,20),
        bodyLarge=style(16,24),bodyMedium=style(14,20),bodySmall=style(13,18),
        labelLarge=style(16,24),labelMedium=style(13,20),labelSmall=style(12,18))
}
/** C direction: content-width text actions, with a stable accessible hit area. */
@Composable
internal fun CAction(text:String,enabled:Boolean=true,arrow:Boolean=false,modifier:Modifier=Modifier,onClick:()->Unit) {
    TextButton(onClick=onClick,enabled=enabled,modifier=modifier.heightIn(min=if(BuildConfig.IS_VEHICLE) 64.dp else 48.dp),
        shape=RoundedCornerShape(4.dp),contentPadding=PaddingValues(horizontal=8.dp,vertical=12.dp)) {
        Text(text,fontSize=if(BuildConfig.IS_VEHICLE) 20.sp else 15.sp,fontWeight=FontWeight.Normal)
        if(arrow) {
            Spacer(Modifier.width(12.dp))
            Icon(painterResource(R.drawable.symbol_arrow_back),null,Modifier.size(20.dp).graphicsLayer(rotationZ=180f))
        }
    }
}
@Composable
internal fun CSection(label:String,title:String,description:String) {
    Text(label,style=MaterialTheme.typography.bodySmall,color=MutedInk)
    Spacer(Modifier.height(8.dp))
    Text(title,style=MaterialTheme.typography.headlineMedium)
    if(description.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Text(description,style=MaterialTheme.typography.bodyMedium,color=MutedInk)
    }
}
@Composable
fun AppSymbol(resource:Int,description:String?=null) {
    Icon(painterResource(resource),description,Modifier.size(if(BuildConfig.IS_VEHICLE) 32.dp else 24.dp))
}
