package com.example.dz1

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dz1.ui.theme.DZ1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DZ1Theme {
                CardListScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen() {
    var itemCount by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (itemCount < 15) {
                        itemCount++
                    }
                    else {
                        Toast.makeText(context, context.getString(R.string.message), Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add card"
                )
            }
        }
    ) { padding ->
        val columns = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) 3 else 4

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(itemCount) { index ->
                CardItem(number = index + 1, {Toast.makeText(context, "$it", Toast.LENGTH_SHORT).show()})
            }
        }
    }
}

@Composable
fun CardItem(number: Int, arg_fun: (Int) -> Unit){
    val backgroundColor = if (number % 2 == 0) Color.Red else Color.Blue
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .padding(8.dp)
            .aspectRatio(1f)
            .clickable{
                arg_fun(number)
            },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DZ1Theme {
        CardListScreen()
    }
}