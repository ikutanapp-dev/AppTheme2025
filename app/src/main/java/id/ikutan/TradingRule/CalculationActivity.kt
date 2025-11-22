package id.ikutan.TradingRule

import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

class CalculationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CalculationScreen()
                }
            }
        }
    }
}

// --- Data Classes for Dynamic Lists ---
data class LiquidationItem(var price: String, var size: String)
data class LiquidationRadiusItem(var start: String, var end: String)
data class WallHeatMapItem(var price: String, var size: String)
data class MarginWallHeatMapItem(var price: String, var size: String)
data class PricePoint(val value: Float, val type: PriceType, val label: String, val size: String? = null)
enum class PriceType {
    CURRENT, DAILY_RES, DAILY_SUP, H4_RES, H4_SUP, LIQ_PRICE, LIQ_RADIUS, WALL, MARGIN_WALL, TRADE_SETUP
}

@Composable
fun CalculationScreen() {
    // --- State Management ---
    var chkEma by remember { mutableStateOf(false) }
    var chkRsi by remember { mutableStateOf(false) }
    var chkEma4h by remember { mutableStateOf(false) }
    var isOverBought by remember { mutableStateOf(false) }
    var isOverSold by remember { mutableStateOf(false) }
    var isMethodDropdownExpanded by remember { mutableStateOf(false) }
    var dailyHorizonResPrice by remember { mutableStateOf("") }
    var dailyTrendResPrice by remember { mutableStateOf("") }
    var dailyHorizonSupPrice by remember { mutableStateOf("") }
    var dailyTrendSupPrice by remember { mutableStateOf("") }
    var h4HorizonResPrice by remember { mutableStateOf("") }
    var h4TrendResPrice by remember { mutableStateOf("") }
    var h4HorizonSupPrice by remember { mutableStateOf("") }
    var h4TrendSupPrice by remember { mutableStateOf("") }
    var currentPrice by remember { mutableStateOf("") }
    var maxBalance by remember { mutableStateOf("") }
    var sizeOnTrade by remember { mutableStateOf("") }
    var openPrice by remember { mutableStateOf("") }
    var slPrice by remember { mutableStateOf("") }
    var tpPrice by remember { mutableStateOf("") }
    var isAlready8h by remember { mutableStateOf(false) }
    var tradeStep by remember { mutableStateOf("") }
    var bnbUsdt by remember { mutableStateOf("") }
    var bnbUsdc by remember { mutableStateOf("") }
    var bybit by remember { mutableStateOf("") }
    var okx by remember { mutableStateOf("") }
    var bitget by remember { mutableStateOf("") }
    var mexc by remember { mutableStateOf("") }
    var gate by remember { mutableStateOf("") }
    var htx by remember { mutableStateOf("") }
    var cbase by remember { mutableStateOf("") }

    val liquidationItems = remember { mutableStateListOf(LiquidationItem("", "")) }
    val liquidationRadiusItems = remember { mutableStateListOf(LiquidationRadiusItem("", "")) }
    val wallHeatMapItems = remember { mutableStateListOf(WallHeatMapItem("", "")) }
    val marginWallHeatMapItems = remember { mutableStateListOf(MarginWallHeatMapItem("", "")) }
    
    var pricePoints by remember { mutableStateOf<List<PricePoint>>(emptyList()) }

    // --- Calculation Logic ---
    val directionPoint by remember(chkEma, chkRsi, chkEma4h) { derivedStateOf { (if (chkEma) 1 else -1) + (if (chkRsi) 1 else -1) + (if (chkEma4h) 1 else -1) } }
    val trendDirectionValue by remember(directionPoint) {
        derivedStateOf {
            when {
                directionPoint < -1 -> -2
                directionPoint == -1 -> -1
                directionPoint == 1 -> 1
                directionPoint > 1 -> 2
                else -> 0
            }
        }
    }
    val trendDirection by remember(trendDirectionValue) {
        derivedStateOf {
            when (trendDirectionValue) {
                -2 -> "Strong Bear"
                -1 -> "Bear"
                1 -> "Bull"
                2 -> "Strong Bull"
                else -> "Neutral"
            }
        }
    }

    val tradingMethods = remember(isOverSold, isOverBought) {
        val baseMethods = mutableListOf("find jump point", "break to fly", "find launch point", "break to ground")
        if (isOverSold) baseMethods.add("wait on dippy")
        if (isOverBought) baseMethods.add("wait on toppy")
        baseMethods
    }
    var selectedMethod by remember { mutableStateOf(tradingMethods.first()) }
    if (selectedMethod !in tradingMethods) selectedMethod = tradingMethods.first()

    val sumFunding by remember(bnbUsdt, bnbUsdc, bybit, okx, bitget, mexc, gate, htx, cbase) {
        derivedStateOf {
            val bnbUsdtValue = bnbUsdt.toDoubleOrNull() ?: 0.0
            val bnbUsdcValue = bnbUsdc.toDoubleOrNull() ?: 0.0
            val bybitValue = bybit.toDoubleOrNull() ?: 0.0
            val okxValue = okx.toDoubleOrNull() ?: 0.0
            val bitgetValue = bitget.toDoubleOrNull() ?: 0.0
            val mexcValue = mexc.toDoubleOrNull() ?: 0.0
            val gateValue = gate.toDoubleOrNull() ?: 0.0
            val htxValue = htx.toDoubleOrNull() ?: 0.0
            val cbaseValue = cbase.toDoubleOrNull() ?: 0.0

            ((bnbUsdtValue * 0.25) + (bnbUsdcValue * 0.05) + (bybitValue * 0.15) +
            (okxValue * 0.125) + (bitgetValue * 0.1) + (mexcValue * 0.125) +
            (gateValue * 0.1) + (htxValue * 0.05) + (cbaseValue * 0.05)) / 1
        }
    }

    val trendDirection2Value by remember(sumFunding) {
        derivedStateOf {
            when {
                sumFunding > 100 -> -2
                sumFunding > 40 -> -1
                sumFunding >= -30 -> 1
                else -> 2
            }
        }
    }

    val trendDirection2 by remember(trendDirection2Value) {
        derivedStateOf {
            when (trendDirection2Value) {
                -2 -> "Strong Bear"
                -1 -> "Bear"
                1 -> "Bull"
                else -> "Strong Bull"
            }
        }
    }

    val suggestPosition by remember(trendDirectionValue, trendDirection2Value) {
        derivedStateOf {
            val sum = trendDirectionValue + trendDirection2Value
            when (sum) {
                -4 -> "100% Short"
                -3 -> "75% Short"
                -2 -> "50% Short"
                -1 -> "25% Short"
                0 -> "Neutral"
                1 -> "25% Long"
                2 -> "50% Long"
                3 -> "75% Long"
                4 -> "100% Long"
                else -> "Neutral"
            }
        }
    }

    // --- UI SECTIONS ---
    FlowDirectionSection(chkEma, { chkEma = it }, chkRsi, { chkRsi = it }, chkEma4h, { chkEma4h = it }, trendDirection)
    OverRsiSection(isOverBought, { isOverBought = it }, isOverSold, { isOverSold = it })
    TradingMethodSection(isMethodDropdownExpanded, { isMethodDropdownExpanded = it }, selectedMethod, { selectedMethod = it }, tradingMethods)
    ResistanceSupportSection(
        dailyHorizonResPrice, { dailyHorizonResPrice = it }, dailyTrendResPrice, { dailyTrendResPrice = it },
        dailyHorizonSupPrice, { dailyHorizonSupPrice = it }, dailyTrendSupPrice, { dailyTrendSupPrice = it },
        h4HorizonResPrice, { h4HorizonResPrice = it }, h4TrendResPrice, { h4TrendResPrice = it },
        h4HorizonSupPrice, { h4HorizonSupPrice = it }, h4TrendSupPrice, { h4TrendSupPrice = it }
    )
    LiquidationPriceSection(liquidationItems) { liquidationItems.add(LiquidationItem("", "")) }
    LiquidationRadiusSection(liquidationRadiusItems) { liquidationRadiusItems.add(LiquidationRadiusItem("", "")) }
    WallHeatMapSection(wallHeatMapItems) { wallHeatMapItems.add(WallHeatMapItem("", "")) }
    MarginWallHeatMapSection(marginWallHeatMapItems) { marginWallHeatMapItems.add(MarginWallHeatMapItem("", "")) }
    CurrentPriceSection(currentPrice) { currentPrice = it }
    SizeSetUpSection(
        maxBalance, { maxBalance = it },
        sizeOnTrade, { sizeOnTrade = it },
        openPrice, { openPrice = it },
        slPrice, { slPrice = it },
        tpPrice, { tpPrice = it }
    )
    AdditionalParameterSection(isAlready8h, { isAlready8h = it }, tradeStep, { tradeStep = it })
    FundingRateSection(bnbUsdt, { bnbUsdt = it }, bnbUsdc, { bnbUsdc = it }, bybit, { bybit = it }, okx, { okx = it }, bitget, { bitget = it }, mexc, { mexc = it }, gate, { gate = it }, htx, { htx = it }, cbase, { cbase = it })
    AverageFundingRateSection(sumFunding, trendDirection2)

    Text("Recommendation Position: $suggestPosition", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)

    Button(onClick = {
        val points = mutableListOf<PricePoint>()
        // Add all prices to a list
        dailyHorizonResPrice.toFloatOrNull()?.let { points.add(PricePoint(it, PriceType.DAILY_RES, "Daily Horizontal Resistance")) }
        dailyTrendResPrice.toFloatOrNull()?.let { points.add(PricePoint(it, PriceType.DAILY_RES, "Daily Trend Resistance")) }
        dailyHorizonSupPrice.toFloatOrNull()?.let { points.add(PricePoint(it, PriceType.DAILY_SUP, "Daily Horizontal Support")) }
        dailyTrendSupPrice.toFloatOrNull()?.let { points.add(PricePoint(it, PriceType.DAILY_SUP, "Daily Trend Support")) }
        h4HorizonResPrice.toFloatOrNull()?.let { points.add(PricePoint(it, PriceType.H4_RES, "4H Horizontal Resistance")) }
        h4TrendResPrice.toFloatOrNull()?.let { points.add(PricePoint(it, PriceType.H4_RES, "4H Trend Resistance")) }
        h4HorizonSupPrice.toFloatOrNull()?.let { points.add(PricePoint(it, PriceType.H4_SUP, "4H Horizontal Support")) }
        h4TrendSupPrice.toFloatOrNull()?.let { points.add(PricePoint(it, PriceType.H4_SUP, "4H Trend Support")) }
        currentPrice.toFloatOrNull()?.let { points.add(PricePoint(it, PriceType.CURRENT, "Current Price")) }
        openPrice.toFloatOrNull()?.let { points.add(PricePoint(it, PriceType.TRADE_SETUP, "Open Price")) }
        slPrice.toFloatOrNull()?.let { points.add(PricePoint(it, PriceType.TRADE_SETUP, "SL Price")) }
        tpPrice.toFloatOrNull()?.let { points.add(PricePoint(it, PriceType.TRADE_SETUP, "TP Price")) }
        liquidationItems.forEachIndexed { i, item -> item.price.toFloatOrNull()?.let { points.add(PricePoint(it, PriceType.LIQ_PRICE, "Liquidation Price ${i + 1}", item.size)) } }
        liquidationRadiusItems.forEachIndexed { i, item ->
            item.start.toFloatOrNull()?.let { points.add(PricePoint(it, PriceType.LIQ_RADIUS, "Liquidation Start ${i + 1}")) }
            item.end.toFloatOrNull()?.let { points.add(PricePoint(it, PriceType.LIQ_RADIUS, "Liquidation End ${i + 1}")) }
        }
        wallHeatMapItems.forEachIndexed { i, item -> item.price.toFloatOrNull()?.let { points.add(PricePoint(it, PriceType.WALL, "Wall Price ${i + 1}", item.size)) } }
        marginWallHeatMapItems.forEachIndexed { i, item -> item.price.toFloatOrNull()?.let { points.add(PricePoint(it, PriceType.MARGIN_WALL, "Margin Wall Price ${i + 1}", item.size)) } }
        
        pricePoints = points.sortedBy { it.value }
    }) {
        Text("Create Planing Map", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
    }

    if (pricePoints.isNotEmpty()) {
        PlaningMap(pricePoints)
        PlaningMapLegend(pricePoints)
    }
}

// --- COMPOSABLE SECTIONS ---

fun getPriceColor(type: PriceType): Color {
    return when (type) {
        PriceType.CURRENT -> Color.Black
        PriceType.DAILY_RES -> Color(0xFF8B0000) // Dark Red
        PriceType.H4_RES -> Color.Red
        PriceType.DAILY_SUP -> Color(0xFF006400) // Dark Green
        PriceType.H4_SUP -> Color(0xFF90EE90)    // Light Green
        PriceType.LIQ_PRICE -> Color(0xFFFFA500) // Orange
        PriceType.LIQ_RADIUS -> Color.Yellow
        PriceType.WALL -> Color.Blue
        PriceType.MARGIN_WALL -> Color(0xFF800080) // Purple
        PriceType.TRADE_SETUP -> Color.Cyan
    }
}

@Composable
fun PlaningMapLegend(points: List<PricePoint>) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Keterangan:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        points.forEach { point ->
            val legendText = if (!point.size.isNullOrBlank()) {
                "${point.label} = ${point.value} (${point.size})"
            } else {
                "${point.label} = ${point.value}"
            }
            Text(
                text = legendText,
                color = getPriceColor(point.type),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PlaningMap(points: List<PricePoint>) {
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(500.dp)
        .padding(vertical = 20.dp)) { 
        
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerX = canvasWidth / 3f

        drawLine(start = Offset(x = centerX, y = 0f), end = Offset(x = centerX, y = canvasHeight), color = Color.Black, strokeWidth = 5f)

        val pointCount = points.size
        if (pointCount == 0) return@Canvas

        val segmentHeight = if (pointCount > 1) canvasHeight / (pointCount - 1) else canvasHeight / 2f

        points.forEachIndexed { index, point ->
            val yOffset = canvasHeight - (segmentHeight * index)
            val color = getPriceColor(point.type)

            drawCircle(color = color, radius = 15f, center = Offset(x = centerX, y = yOffset))

            val textToDraw = if (!point.size.isNullOrBlank()) {
                "${point.value} (${point.size})"
            } else {
                point.value.toString()
            }

            drawContext.canvas.nativeCanvas.drawText(
                textToDraw,
                centerX + 40f, 
                yOffset + 10f, 
                Paint().apply {
                    this.color = android.graphics.Color.BLACK
                    textAlign = Paint.Align.LEFT
                    textSize = 35f
                }
            )
        }
    }
}

@Composable
fun FlowDirectionSection(chkEma: Boolean, onEmaChange: (Boolean) -> Unit, chkRsi: Boolean, onRsiChange: (Boolean) -> Unit, chkEma4h: Boolean, onEma4hChange: (Boolean) -> Unit, trendDirection: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Flow Direction", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(chkEma, onEmaChange); Text("Is EMA Daily 9 > 20?", fontSize = 12.sp) }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(chkRsi, onRsiChange); Text("Is RSI Daily 20 > 50?", fontSize = 12.sp) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(chkEma4h, onEma4hChange); Text("Is EMA 4h 9 > 20?", fontSize = 12.sp) }
        val trendColor = if (trendDirection.contains("Bear")) Color(0xFF8B0000) else Color(0xFF006400)
        Text(buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) { append("Trend: ") }
            withStyle(SpanStyle(color = trendColor)) { append(trendDirection) }
        })
    }
}

@Composable
fun OverRsiSection(isOverBought: Boolean, onOverBoughtChange: (Boolean) -> Unit, isOverSold: Boolean, onOverSoldChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Over RSI Daily", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(isOverBought, onOverBoughtChange); Text("Is RSI 9 daily > 80?", fontSize = 12.sp) }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(isOverSold, onOverSoldChange); Text("Is RSI 9 daily < 30?", fontSize = 12.sp) }
        }
    }
}

@Composable
fun TradingMethodSection(isExpanded: Boolean, onExpandedChange: (Boolean) -> Unit, selectedMethod: String, onMethodSelected: (String) -> Unit, tradingMethods: List<String>) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Trading Method", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        Box {
            OutlinedButton(onClick = { onExpandedChange(true) }) { Text(selectedMethod) }
            DropdownMenu(expanded = isExpanded, onDismissRequest = { onExpandedChange(false) }) {
                tradingMethods.forEach { method -> DropdownMenuItem(text = { Text(method) }, onClick = { onMethodSelected(method); onExpandedChange(false) }) }
            }
        }
    }
}

@Composable
fun ResistanceSupportSection(dailyHorizonResPrice: String, onDailyHorizonResPriceChange: (String) -> Unit, dailyTrendResPrice: String, onDailyTrendResPriceChange: (String) -> Unit, dailyHorizonSupPrice: String, onDailyHorizonSupPriceChange: (String) -> Unit, dailyTrendSupPrice: String, onDailyTrendSupPriceChange: (String) -> Unit, h4HorizonResPrice: String, onH4HorizonResPriceChange: (String) -> Unit, h4TrendResPrice: String, onH4TrendResPriceChange: (String) -> Unit, h4HorizonSupPrice: String, onH4HorizonSupPriceChange: (String) -> Unit, h4TrendSupPrice: String, onH4TrendSupPriceChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Daily Resistance and Support", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            TextField(dailyHorizonResPrice, onDailyHorizonResPriceChange, label = { Text("Horizontal Resistance 1d") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            TextField(dailyTrendResPrice, onDailyTrendResPriceChange, label = { Text("TrendLine Resistance 1d") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            TextField(dailyHorizonSupPrice, onDailyHorizonSupPriceChange, label = { Text("Horizontal Support 1d") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            TextField(dailyTrendSupPrice, onDailyTrendSupPriceChange, label = { Text("TrendLine Support 1d") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Text("4H Resistance and Support", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            TextField(h4HorizonResPrice, onH4HorizonResPriceChange, label = { Text("Horizontal Resistance 4h") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            TextField(h4TrendResPrice, onH4TrendResPriceChange, label = { Text("TrendLine Resistance 4h") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            TextField(h4HorizonSupPrice, onH4HorizonSupPriceChange, label = { Text("Horizontal Support 4h") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            TextField(h4TrendSupPrice, onH4TrendSupPriceChange, label = { Text("TrendLine Support 4h") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
    }
}

@Composable
fun LiquidationPriceSection(items: MutableList<LiquidationItem>, onAddItem: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Liquidation Price", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        items.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(item.price, { items[index] = item.copy(price = it) }, label = { Text("Liquidation Price ${index + 1}") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                TextField(item.size, { items[index] = item.copy(size = it) }, label = { Text("Liquidation Size ${index + 1}") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Text("Million")
            }
        }
        Button(onClick = onAddItem) { Text("Add More") }
    }
}

@Composable
fun LiquidationRadiusSection(items: MutableList<LiquidationRadiusItem>, onAddItem: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Liquidation Radius", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        items.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                TextField(item.start, { items[index] = item.copy(start = it) }, label = { Text("Liquidation Start ${index + 1}") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                TextField(item.end, { items[index] = item.copy(end = it) }, label = { Text("Liquidation End ${index + 1}") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
        Button(onClick = onAddItem) { Text("Add More") }
    }
}

@Composable
fun WallHeatMapSection(items: MutableList<WallHeatMapItem>, onAddItem: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Wall Heat Map", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        items.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(item.price, { items[index] = item.copy(price = it) }, label = { Text("Wall Price ${index + 1}") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                TextField(item.size, { items[index] = item.copy(size = it) }, label = { Text("Wall Size ${index + 1}") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Text("BTC")
            }
        }
        Button(onClick = onAddItem) { Text("Add More") }
    }
}

@Composable
fun MarginWallHeatMapSection(items: MutableList<MarginWallHeatMapItem>, onAddItem: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Margin Wall Heat Map", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        items.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(item.price, { items[index] = item.copy(price = it) }, label = { Text("Margin Wall Price ${index + 1}") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                TextField(item.size, { items[index] = item.copy(size = it) }, label = { Text("Margin Wall Size ${index + 1}") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Text("Million")
            }
        }
        Button(onClick = onAddItem) { Text("Add More") }
    }
}

@Composable
fun CurrentPriceSection(price: String, onPriceChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Current Price", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        TextField(price, onPriceChange, label = { Text("BTC Price") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    }
}

@Composable
fun SizeSetUpSection(
    maxBalance: String, onMaxBalanceChange: (String) -> Unit,
    sizeOnTrade: String, onSizeOnTradeChange: (String) -> Unit,
    openPrice: String, onOpenPriceChange: (String) -> Unit,
    slPrice: String, onSlPriceChange: (String) -> Unit,
    tpPrice: String, onTpPriceChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Size Set Up", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            TextField(maxBalance, onMaxBalanceChange, label = { Text("Max Balance (USDT)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            TextField(sizeOnTrade, onSizeOnTradeChange, label = { Text("Size on Trade") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            TextField(openPrice, onOpenPriceChange, label = { Text("Open Price") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            TextField(slPrice, onSlPriceChange, label = { Text("SL Price") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            TextField(tpPrice, onTpPriceChange, label = { Text("TP Price") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
    }
}

@Composable
fun AdditionalParameterSection(
    isAlready8h: Boolean, onIsAlready8hChange: (Boolean) -> Unit,
    tradeStep: String, onTradeStepChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Additional Parameter", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(checked = isAlready8h, onCheckedChange = onIsAlready8hChange)
            Text("Is Already 8h?")
            TextField(tradeStep, onTradeStepChange, label = { Text("Trade Step") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
    }
}

@Composable
fun FundingRateSection(
    bnbUsdt: String, onBnbUsdtChange: (String) -> Unit,
    bnbUsdc: String, onBnbUsdcChange: (String) -> Unit,
    bybit: String, onBybitChange: (String) -> Unit,
    okx: String, onOkxChange: (String) -> Unit,
    bitget: String, onBitgetChange: (String) -> Unit,
    mexc: String, onMexcChange: (String) -> Unit,
    gate: String, onGateChange: (String) -> Unit,
    htx: String, onHtxChange: (String) -> Unit,
    cbase: String, onCbaseChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Funding Rate", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            TextField(bnbUsdt, onBnbUsdtChange, label = { Text("bnbUsdt") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            TextField(bnbUsdc, onBnbUsdcChange, label = { Text("bnbUsdc") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            TextField(bybit, onBybitChange, label = { Text("bybit") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            TextField(okx, onOkxChange, label = { Text("okx") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            TextField(bitget, onBitgetChange, label = { Text("bitget") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            TextField(mexc, onMexcChange, label = { Text("mexc") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            TextField(gate, onGateChange, label = { Text("gate") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            TextField(htx, onHtxChange, label = { Text("htx") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            TextField(cbase, onCbaseChange, label = { Text("cbase") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Text("usdtperp, usdcperp, usdtCoin, usdtFut1, usdtFut2", fontSize = 12.sp, fontStyle = FontStyle.Italic)
    }
}

@Composable
fun AverageFundingRateSection(average: Double, trendDirection: String) {
    val color = if (average < 0) Color.Red else Color.Green
    val formattedAverage = DecimalFormat("#.####").format(average)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Avg Funding rate : $formattedAverage%", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        // Changed this part to split Trend 2 into a separate Text composable with the requested styling
        val trendColor = if (trendDirection.contains("Bear")) Color(0xFF8B0000) else Color(0xFF006400)
        Text(buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) { append("Trend 2 : ") }
            withStyle(SpanStyle(color = trendColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)) { append(trendDirection) }
        })
    }
}
