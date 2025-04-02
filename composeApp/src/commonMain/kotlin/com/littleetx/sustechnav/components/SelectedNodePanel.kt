package com.littleetx.sustechnav.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.littleetx.sustechnav.data.NodeInfo
import io.github.dellisd.spatialk.geojson.Position
import net.sergeych.sprintf.sprintf

@Composable
fun SelectedNodePanel(
    node: NodeInfo,
    isEditing: Boolean,
    setIsEditing: (Boolean) -> Unit,
    selectedNodeNewPosition: Position,
    setSelectedNodeNewPosition: (Position) -> Unit,
    onModifyNode: (NodeInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (!isEditing) {
                var expanded by remember { mutableStateOf(false) }
                IconButton(
                    modifier = Modifier.align(Alignment.TopEnd),
                    onClick = { expanded = !expanded }
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")

                    DropdownMenu(
                        modifier = Modifier.align(Alignment.TopEnd),
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = "Edit") },
                            text = { Text("编辑节点") },
                            onClick = {
                                setIsEditing(true)
                                setSelectedNodeNewPosition(node.position)
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = "Delete") },
                            text = { Text("删除节点") },
                            colors = MenuDefaults.itemColors().copy(
                                textColor = MaterialTheme.colorScheme.error,
                                leadingIconColor = MaterialTheme.colorScheme.error,
                            ),
                            onClick = {
                                //TODO
                            }
                        )
                    }
                }
            } else {
                IconButton(
                    modifier = Modifier.align(Alignment.TopEnd),
                    onClick = {
                        setIsEditing(false)
                        onModifyNode(NodeInfo(
                            id = node.id,
                            position = selectedNodeNewPosition,
                        ))
                    }
                ) {
                    Icon(Icons.Filled.Done, contentDescription = "Done")
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text("节点 ID: ${node.id}")
                Row {
                    Text("经度：${"%3.7f".sprintf(node.position.latitude)}")
                    if (isEditing) {
                        Icon(Icons.Filled.KeyboardDoubleArrowRight,
                            contentDescription = "To",
                            tint = Color.Green,
                        )
                        Text("%3.7f".sprintf(selectedNodeNewPosition.latitude),
                            color = Color.Green,
                        )
                    }
                }
                Row {
                    Text("纬度：${"%3.7f".sprintf(node.position.longitude)}")
                    if (isEditing) {
                        Icon(Icons.Filled.KeyboardDoubleArrowRight,
                            contentDescription = "To",
                            tint = Color.Green,
                        )
                        Text("%3.7f".sprintf(selectedNodeNewPosition.longitude),
                            color = Color.Green,
                        )
                    }
                }
            }
        }
    }
}