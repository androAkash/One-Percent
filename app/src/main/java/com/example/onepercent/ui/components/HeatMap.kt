package com.example.onepercent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class HeatMapEntry(
    val epochMillis: Long,
    val importantTaskCount: Int
) {
    val localDate: LocalDate
        get() = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
}
object HeatMapColorScale {
    fun getColor(taskCount: Int?): Color {
        return when {
            taskCount == null || taskCount == 0 -> Color(0xFFF5F5F5) // No Data
            taskCount <= 2 -> Color(0xFFA3E635)
            taskCount <= 5 -> Color(0xFF4ADE80)
            else -> Color(0xFF15803D)
        }
    }
}
@Composable
fun HeatMapCalendar(
    data: List<HeatMapEntry>,
    year: Int = LocalDate.now().year,
    modifier: Modifier = Modifier,
    onCellClick: ((HeatMapEntry?) -> Unit)? = null
) {
    // Group data by date for quick lookup
    val dataMap = remember(data) {
        data.associateBy { it.localDate }
    }

    // Get all months for the year
    val months = remember(year) {
        (1..12).map { monthNum ->
            YearMonth.of(year, monthNum)
        }
    }

    var selectedEntry by remember { mutableStateOf<HeatMapEntry?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(top = 32.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach { day ->
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .width(40.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = day,
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Scrollable months section
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                months.forEach { month ->
                    MonthHeatMap(
                        yearMonth = month,
                        dataMap = dataMap,
                        onCellClick = { entry ->
                            selectedEntry = entry
                            onCellClick?.invoke(entry)
                        }
                    )
                }
            }
        }
        // Selected cell info
        if (selectedEntry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            SelectedCellInfo(entry = selectedEntry!!)
        }
    }
}
@Composable
fun MonthHeatMap(
    yearMonth: YearMonth,
    dataMap: Map<LocalDate, HeatMapEntry>,
    onCellClick: (HeatMapEntry?) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Month name header
        Text(
            text = yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Get the first and last day of the month
        val firstDayOfMonth = yearMonth.atDay(1)
        val lastDayOfMonth = yearMonth.atEndOfMonth()
        val daysInMonth = yearMonth.lengthOfMonth()

        // Get the day of week for the first day (1 = Monday, 7 = Sunday)
        val startDayOfWeek = firstDayOfMonth.dayOfWeek.value

        // Calculate number of weeks (columns) needed
        // We need to know which week the month starts and ends
        val firstWeekOffset = startDayOfWeek - 1 // 0-6, how many days into the week we start
        val totalCells = firstWeekOffset + daysInMonth
        val numberOfWeeks = (totalCells + 6) / 7

        // Create grid: 7 rows (Mon-Sun) x numberOfWeeks columns
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Each row represents a day of the week (Monday = 0, Sunday = 6)
            repeat(7) { dayOfWeekIndex ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Each column represents a week
                    repeat(numberOfWeeks) { weekIndex ->
                        // Calculate which day of the month this cell represents
                        // dayOfWeekIndex: 0=Mon, 1=Tue, 2=Wed, 3=Thu, 4=Fri, 5=Sat, 6=Sun
                        // weekIndex: 0=first week, 1=second week, etc.
                        val cellPosition = weekIndex * 7 + dayOfWeekIndex
                        val dayOfMonth = cellPosition - firstWeekOffset + 1

                        if (dayOfMonth in 1..daysInMonth) {
                            val date = yearMonth.atDay(dayOfMonth)
                            val entry = dataMap[date]
                            val visitors = entry?.importantTaskCount

                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(
                                        color = HeatMapColorScale.getColor(visitors),
                                        shape = RoundedCornerShape(3.dp)
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = Color(0xFFE5E5E5),
                                        shape = RoundedCornerShape(3.dp)
                                    )
                                    .clickable {
                                        onCellClick(entry)
                                    }
                            )
                        } else {
                            // Empty cell for days outside this month
                            Box(modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun SelectedCellInfo(entry: HeatMapEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Selected Date",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.localDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${entry.importantTaskCount}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16A34A)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "tasks",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Preview
@Composable
fun HeatMapCalendarPreview() {
    val sampleData = listOf(
        HeatMapEntry(epoch(2026, 1, 1), 1),
        HeatMapEntry(epoch(2026, 1, 21), 2),
        HeatMapEntry(epoch(2026, 1, 7), 3),
        HeatMapEntry(epoch(2026, 1, 8), 4),
        HeatMapEntry(epoch(2026, 1, 15), 5),
        HeatMapEntry(epoch(2026, 2, 14), 6),
        HeatMapEntry(epoch(2026, 2, 28), 7),
        HeatMapEntry(epoch(2026, 3, 1), 8)
    )
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            HeatMapCalendar(
                data = sampleData,
                year = 2026
            )
        }
    }
}
private fun epoch(y: Int, m: Int, d: Int): Long =
    LocalDate.of(y, m, d)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
