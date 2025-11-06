package com.forgecompose.workouttracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

@Composable
fun WeightHistoryGraph(
    workouts: List<Workout>
) {

    val weighted = remember(workouts) { workouts.filter { (it.weight ?: 0.0) > 0.0 } }


    val byNameSortedAsc = remember(weighted) {
        weighted.groupBy { it.name }.mapValues { (_, list) -> list.sortedBy { it.date } }
    }


    val prevById: Map<Long, Workout?> = remember(byNameSortedAsc) {
        buildMap<Long, Workout?> {
            for ((_, list) in byNameSortedAsc) {
                var last: Workout? = null
                for (w in list) {
                    put(w.id.toLong(), last)
                    last = w
                }
            }
        }
    }


    val maxByName: Map<String, Double> = remember(byNameSortedAsc) {
        byNameSortedAsc.mapValues { (_, list) ->
            list.maxOfOrNull { it.weight ?: 0.0 }?.takeIf { it > 0.0 } ?: 1.0
        }
    }


    val last3 = remember(weighted) { weighted.sortedByDescending { it.date }.take(20) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Recent Weights",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))

            if (last3.isEmpty()) {
                Text(
                    "No workouts with tracked weight found.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                return@Column
            }


            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = false
            ) {
                items(items = last3, key = { it.id }) { w ->
                    val prevSame = prevById[w.id.toLong()]
                    val maxForThis = maxByName[w.name] ?: 1.0
                    GraphBar(
                        workout = w,
                        maxWeight = maxForThis,
                        prevSameNameWeight = prevById[w.id.toLong()]?.weight,   // primary comparison
                        baselineWeight = null                           // or fallback: personal record / rolling avg
                    )

                }
            }
        }
    }
}