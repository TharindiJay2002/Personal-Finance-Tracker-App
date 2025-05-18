package com.example.trackmyfunds

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class AnalysisActivity : AppCompatActivity() {

    private lateinit var incomeBarChart: BarChart
    private lateinit var expenseBarChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis)

        incomeBarChart = findViewById(R.id.incomeBarChart)
        expenseBarChart = findViewById(R.id.expenseBarChart)

        setupCharts()
        displayBarCharts()
    }

    private fun setupCharts() {
        listOf(incomeBarChart, expenseBarChart).forEach { chart ->
            chart.apply {
                description.isEnabled = false
                setDrawGridBackground(false)
                setBorderColor(Color.WHITE)
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(false)
                setDrawBarShadow(false)
                setDrawValueAboveBar(true)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    textColor = ContextCompat.getColor(context, R.color.accent_secondary)
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    textColor = ContextCompat.getColor(context, R.color.accent_secondary)
                }

                axisRight.isEnabled = false
                legend.isEnabled = true
                legend.textColor = ContextCompat.getColor(context, R.color.accent_secondary)
            }
        }
    }

    private fun displayBarCharts() {
        val sharedPreferences = getSharedPreferences("TrackMyFundsPrefs", MODE_PRIVATE)
        val transactions = sharedPreferences.getStringSet("transactions", mutableSetOf())!!
            .map { it.split("|") }

        val incomeByCategory = transactions
            .filter { it[0] == "Income" }
            .groupBy { it[4] } // Group by category
            .mapValues { it.value.sumOf { transaction -> transaction[3].toFloat().toDouble() } }

        val expenseByCategory = transactions
            .filter { it[0] == "Expense" }
            .groupBy { it[4] }
            .mapValues { it.value.sumOf { transaction -> transaction[3].toFloat().toDouble() } }

        setupBarChart(incomeBarChart, incomeByCategory, "Income by Category",
            ContextCompat.getColor(this, android.R.color.holo_green_light))
        setupBarChart(expenseBarChart, expenseByCategory, "Expense by Category",
            ContextCompat.getColor(this, android.R.color.holo_red_light))
    }

    private fun setupBarChart(chart: BarChart, data: Map<String, Double>, label: String, color: Int) {
        val entries = data.values.mapIndexed { index, value ->
            BarEntry(index.toFloat(), value.toFloat())
        }

        val dataSet = BarDataSet(entries, label).apply {
            this.color = color
            valueTextColor = ContextCompat.getColor(this@AnalysisActivity, R.color.accent_secondary)
            valueTextSize = 12f
        }

        chart.apply {
            this.data = BarData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(data.keys.toList())
            xAxis.labelRotationAngle = 45f
            notifyDataSetChanged()
            invalidate()
        }
    }
}