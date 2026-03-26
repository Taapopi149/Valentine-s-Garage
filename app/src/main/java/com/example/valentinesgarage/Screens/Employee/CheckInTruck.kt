package com.example.valentinesgarage.Screens.CheckIn

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter


// ─── Data ────────────────────────────────────────────────────────────────────

enum class VehicleCondition(val label: String, val filledSegments: Int) {
    EXCELLENT("Excellent", 4),
    GOOD("Good", 3),
    FAIR("Fair", 2),
    POOR("Poor", 1)
}

data class TruckCheckInForm(
    val licencePlate: String = "",
    val driverName: String = "",
    val odometer: String = "",
    val condition: VehicleCondition = VehicleCondition.GOOD,
    val photoUris: List<Uri> = emptyList(),
    val notes: String = ""
)

// ─── ViewModel ───────────────────────────────────────────────────────────────
// Swap the TODO in submitCheckIn() for your Firebase / Room call when ready.

class CheckInViewModel : ViewModel() {

    var form by mutableStateOf(TruckCheckInForm())
        private set

    fun onLicencePlateChange(value: String) { form = form.copy(licencePlate = value) }
    fun onDriverNameChange(value: String)   { form = form.copy(driverName = value) }
    fun onOdometerChange(value: String)     { form = form.copy(odometer = value.filter { it.isDigit() }) }
    fun onConditionChange(c: VehicleCondition) { form = form.copy(condition = c) }
    fun onPhotosAdded(uris: List<Uri>)      { form = form.copy(photoUris = (form.photoUris + uris).take(5)) }
    fun onNotesChange(value: String)        { form = form.copy(notes = value) }

    fun submitCheckIn(onSuccess: () -> Unit) {
        // TODO: replace with Firebase Firestore or Room insert
        // e.g. FirebaseFirestore.getInstance().collection("checkIns").add(form.toMap())
        println("Check-in submitted: $form")
        onSuccess()
    }

    fun isFormValid(): Boolean =
        form.licencePlate.isNotBlank() &&
                form.driverName.isNotBlank() &&
                form.odometer.isNotBlank()
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruckCheckInScreen(
    navController: NavController,
    viewModel: CheckInViewModel = viewModel()
) {
    val form = viewModel.form

    // Image picker — allows multiple selections
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> viewModel.onPhotosAdded(uris) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Truck Check-In") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            // ── Licence plate ─────────────────────────────────────────────
            FieldLabel("Licence plate")
            OutlinedTextField(
                value = form.licencePlate,
                onValueChange = viewModel::onLicencePlateChange,
                placeholder = { Text("e.g. N 12345 W") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(Modifier.height(8.dp))

            // ── Driver name ───────────────────────────────────────────────
            FieldLabel("Driver name")
            OutlinedTextField(
                value = form.driverName,
                onValueChange = viewModel::onDriverNameChange,
                placeholder = { Text("Full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(Modifier.height(8.dp))

            // ── Odometer ──────────────────────────────────────────────────
            FieldLabel("Odometer (km)")
            OutlinedTextField(
                value = form.odometer,
                onValueChange = viewModel::onOdometerChange,
                placeholder = { Text("e.g. 148302") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(Modifier.height(8.dp))

            // ── Vehicle condition ─────────────────────────────────────────
            FieldLabel("Vehicle condition")
            ConditionSelector(
                selected = form.condition,
                onSelect = viewModel::onConditionChange
            )

            Spacer(Modifier.height(8.dp))

            // ── Photos ────────────────────────────────────────────────────
            FieldLabel("Photos (up to 5)")
            PhotoPicker(
                uris = form.photoUris,
                onAddPhotos = { photoPicker.launch("image/*") }
            )

            Spacer(Modifier.height(8.dp))

            // ── Notes ─────────────────────────────────────────────────────
            FieldLabel("Notes / damage description")
            OutlinedTextField(
                value = form.notes,
                onValueChange = viewModel::onNotesChange,
                placeholder = { Text("Describe any scratches, damage, etc.") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(Modifier.height(20.dp))

            // ── Submit ────────────────────────────────────────────────────
            Button(
                onClick = {
                    viewModel.submitCheckIn {
                        navController.popBackStack()
                    }
                },
                enabled = viewModel.isFormValid(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A1A1A),
                    disabledContainerColor = Color(0xFFCCCCCC)
                )
            ) {
                Text(
                    text = "Confirm Check-In",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Reusable components ──────────────────────────────────────────────────────

@Composable
fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun ConditionSelector(
    selected: VehicleCondition,
    onSelect: (VehicleCondition) -> Unit
) {
    Column {
        // Chip row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VehicleCondition.entries.forEach { condition ->
                val isSelected = condition == selected
                Surface(
                    onClick = { onSelect(condition) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(0xFF1A1A1A) else Color.Transparent,
                    border = BorderStroke(
                        0.5.dp,
                        if (isSelected) Color(0xFF1A1A1A) else Color.LightGray
                    )
                ) {
                    Text(
                        text = condition.label,
                        fontSize = 12.sp,
                        color = if (isSelected) Color.White else Color.Gray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Progress bar showing condition level
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (index < selected.filledSegments) Color(0xFF1A1A1A)
                            else Color(0xFFEEEEEE)
                        )
                )
            }
        }
    }
}

@Composable
fun PhotoPicker(
    uris: List<Uri>,
    onAddPhotos: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

      //   Show existing photo thumbnails
        uris.forEach { uri ->
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "Vehicle photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(0.5.dp, Color.LightGray, RoundedCornerShape(8.dp))
            )
        }

        // Add photo button (show if fewer than 5 photos)
        if (uris.size < 5) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5))
                    .border(
                        BorderStroke(0.5.dp, Color.LightGray),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onAddPhotos() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add photo",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun TruckCheckInPreview() {
    TruckCheckInScreen(navController = rememberNavController())
}