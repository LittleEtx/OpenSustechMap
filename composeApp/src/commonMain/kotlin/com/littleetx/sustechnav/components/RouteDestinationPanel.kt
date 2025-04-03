package com.littleetx.sustechnav.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.littleetx.sustechnav.NodeId
import com.littleetx.sustechnav.data.NavigationData
import com.littleetx.sustechnav.data.distanceTo
import com.littleetx.sustechnav.data.getNode
import net.sergeych.sprintf.sprintf


data class SearchRouteResult(
    val routes: List<NodeId> = emptyList(),
    val totalDistance: Double = 0.0,
) {
    val start get() = routes.first()
    val destination get() = routes.last()
    val isValid get() = routes.size >= 2
}

fun searchRoute(navData: NavigationData, start: NodeId, des: NodeId): SearchRouteResult {
    val distances = mutableMapOf<NodeId, Double>().apply {
        navData.nodes.values.forEach { node -> this[node.id] = Double.POSITIVE_INFINITY }
        this[start] = 0.0
    }
    val previousNodes = mutableMapOf<NodeId, Pair<NodeId, Double>>()
    val unvisitedNodes = navData.nodes.keys.toMutableSet()

    // TODO: 使用优先队列优化
    while (unvisitedNodes.isNotEmpty()) {
        val currentNode = unvisitedNodes.minByOrNull { distances[it]!! } ?: break
        unvisitedNodes -= currentNode

        if (currentNode == des) break // Early exit if we reach destination

        navData.nodeConn[currentNode]?.forEach { conn ->
            val neighbor = conn.target
            val p1 = navData.getNode(currentNode)!!.position
            val p2 = navData.getNode(neighbor)!!.position
            val distance = p1 distanceTo p2

            val newDistance = distances[currentNode]!! + distance
            if (newDistance < distances[neighbor]!!) {
                distances[neighbor] = newDistance
                previousNodes[neighbor] = currentNode to distance
            }
        }
    }

    // Reconstruct path
    val path = mutableListOf<NodeId>()
    var current: NodeId? = des
    var totalDistance = 0.0
    while (current != null) {
        path.add(0, current)
        val prev = previousNodes[current]
        if (prev != null) {
            current = prev.first
            totalDistance += prev.second
        } else {
            current = null
        }
    }

    return SearchRouteResult(
        routes = path.reversed(),
        totalDistance = totalDistance,
    )
}

@Composable
fun RouteDestinationPanel(
    navData: NavigationData,
    modifier: Modifier = Modifier,
    onCreateRoute: (SearchRouteResult) -> Unit,
) {
    var originInput by remember { mutableStateOf("") }
    var destinationInput by remember { mutableStateOf("") }

    val textFieldColor = TextFieldDefaults.colors().copy(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
    )

    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier,
    ) {
        Card(
            colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceBright)
        ) {
            Box {
                Column(
                    modifier = Modifier.padding(end = 50.dp).align(Alignment.CenterStart)
                ) {
                    TextField(
                        colors = textFieldColor,
                        leadingIcon = { Icon(Icons.Filled.NearMe, contentDescription = "start") },
                        value = originInput,
                        onValueChange = { originInput = it.trim() },
                        singleLine = true,
                        modifier = Modifier.height(56.dp).fillMaxWidth()
                    )
                    TextField(
                        colors = textFieldColor,
                        leadingIcon = { Icon(Icons.Filled.Place, contentDescription = "destination") },
                        value = destinationInput,
                        onValueChange = { destinationInput = it.trim() },
                        singleLine = true,
                        modifier = Modifier.height(56.dp).fillMaxWidth()
                    )
                }
                IconButton(
                    modifier = Modifier
                        .padding(end = 5.dp)
                        .width(40.dp)
                        .align(Alignment.CenterEnd)
                    ,
                    onClick = {
                        val temp = originInput
                        originInput = destinationInput
                        destinationInput = temp
                    }
                ) {
                    Icon(Icons.Filled.SwapVert, contentDescription = "Swap")
                }
            }
        }

        var dialogReason by remember { mutableStateOf("") }
        var currentDistance by remember { mutableStateOf(0.0) }
        if (dialogReason.isNotEmpty()) {
            InfoDialog(
                dialogTitle = "无法建立路线",
                dialogText = dialogReason,
                onConfirmation = { dialogReason = "" }
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (currentDistance > 0) {
                Text(
                    "路线长度：%.2f m".sprintf(currentDistance),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceBright,
                            shape = MaterialTheme.shapes.extraLarge
                        )
                        .padding(5.dp)
                )
            }
            Button(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = {
                    val start = navData.pois.find { it.name == originInput }
                    val destination = navData.pois.find { it.name == destinationInput }
                    currentDistance = 0.0
                    if (start == null) {
                        dialogReason = "无法找到地点：${originInput}"
                        return@Button
                    }
                    if (destination == null) {
                        dialogReason = "无法找到地点：${destinationInput}"
                        return@Button
                    }

                    val result = searchRoute(navData, start.node, destination.node)
                    if (!result.isValid) {
                        dialogReason = "无法生成从 ${start.name} 到 ${destination.name} 的路线"
                        return@Button
                    }
                    currentDistance = result.totalDistance
                    onCreateRoute(result)
                    Logger.i { "$result" }
                },
                enabled = originInput.isNotEmpty() && destinationInput.isNotEmpty()
            ) {
                Text("生成路线")
            }
        }

    }


}