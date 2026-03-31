package com.example.valentinesgarage.Screens.Manager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlin.random.Random

// ─── Helper Functions ──────────────────────────────────────────────────────────

fun generateEmployeeId(): String {
    val number = Random.nextInt(1000, 9999)
    return "EMP-$number"
}

fun generatePassword(): String {
    val upper   = ('A'..'Z').toList()
    val lower   = ('a'..'z').toList()
    val digits  = ('0'..'9').toList()
    val special = listOf('!', '@', '#', '$', '%', '&')

    // Guarantee at least one of each character type
    val required = listOf(
        upper.random(),
        upper.random(),
        lower.random(),
        lower.random(),
        digits.random(),
        digits.random(),
        special.random(),
        special.random(),
    )

    // Fill remaining characters from all pools combined
    val allChars = upper + lower + digits + special
    val extra    = (1..(12 - required.size)).map { allChars.random() }

    // Shuffle so required chars aren't always at the front
    return (required + extra).shuffled().joinToString("")
}

// ─── Add Employee Screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmployeeScreen(navController: NavController) {

    val clipboardManager = LocalClipboardManager.current

    // Form state
    var fullName   by remember { mutableStateOf("") }
    var email      by remember { mutableStateOf("") }
    var phone      by remember { mutableStateOf("") }
    var role       by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var salary     by remember { mutableStateOf("") }

    // Generated credentials — created once when the screen opens
    var employeeId       by remember { mutableStateOf(generateEmployeeId()) }
    var generatedPassword by remember { mutableStateOf(generatePassword()) }
    var passwordVisible  by remember { mutableStateOf(false) }
    var idCopied         by remember { mutableStateOf(false) }
    var pwCopied         by remember { mutableStateOf(false) }

    // Dropdown state
    var deptExpanded by remember { mutableStateOf(false) }
    val departments  = listOf("Engineering", "Design", "Product", "Analytics", "Marketing", "HR")

    // Validation errors
    var nameError  by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var roleError  by remember { mutableStateOf(false) }

    // Success dialog
    var showSuccess by remember { mutableStateOf(false) }

    // ── Success Dialog ──────────────────────────────────────────────────────
    if (showSuccess) {
        AlertDialog(
            onDismissRequest = {},
            icon  = { Icon(Icons.Default.Check, contentDescription = null) },
            title = { Text("Employee Added!") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$fullName has been successfully added.")
                    HorizontalDivider()
                    CredentialRow(label = "Employee ID", value = employeeId)
                    CredentialRow(label = "Password",    value = generatedPassword)
                    Text(
                        text  = "Please share these credentials securely with the employee.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { navController.popBackStack() }) {
                    Text("Done")
                }
            }
        )
    }

    // ── Scaffold ────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Add Employee",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector        = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Section: Personal Info ──────────────────────────────────────
            SectionHeader(title = "Personal Information", icon = Icons.Default.Person)

            FormField(
                value         = fullName,
                onValueChange = { fullName = it; nameError = false },
                label         = "Full Name *",
                placeholder   = "e.g. Alice Johnson",
                isError       = nameError,
                errorMessage  = "Full name is required",
                leadingIcon   = Icons.Default.Person,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            FormField(
                value         = email,
                onValueChange = { email = it; emailError = false },
                label         = "Email Address *",
                placeholder   = "e.g. alice@company.com",
                isError       = emailError,
                errorMessage  = "Enter a valid email address",
                leadingIcon   = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            FormField(
                value           = phone,
                onValueChange   = { phone = it },
                label           = "Phone Number",
                placeholder     = "e.g. +264 81 000 0000",
                leadingIcon     = Icons.Default.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            HorizontalDivider()

            // ── Section: Job Details ────────────────────────────────────────
            SectionHeader(title = "Job Details", icon = Icons.Default.Work)

            FormField(
                value         = role,
                onValueChange = { role = it; roleError = false },
                label         = "Job Role / Title *",
                placeholder   = "e.g. Senior Developer",
                isError       = roleError,
                errorMessage  = "Role is required",
                leadingIcon   = Icons.Default.Star,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            // Department Dropdown
            ExposedDropdownMenuBox(
                expanded         = deptExpanded,
                onExpandedChange = { deptExpanded = it }
            ) {
                OutlinedTextField(
                    value         = department,
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Department") },
                    placeholder   = { Text("Select department") },
                    leadingIcon   = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    trailingIcon  = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded)
                    },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape         = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded         = deptExpanded,
                    onDismissRequest = { deptExpanded = false }
                ) {
                    departments.forEach { dept ->
                        DropdownMenuItem(
                            text    = { Text(dept) },
                            onClick = {
                                department   = dept
                                deptExpanded = false
                            }
                        )
                    }
                }
            }

            FormField(
                value           = salary,
                onValueChange   = { salary = it },
                label           = "Salary (optional)",
                placeholder     = "e.g. 15000",
                leadingIcon     = Icons.Default.List,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            HorizontalDivider()

            // ── Section: Generated Credentials ─────────────────────────────
            SectionHeader(title = "Generated Credentials", icon = Icons.Default.Lock)

            // Employee ID field
            OutlinedTextField(
                value         = employeeId,
                onValueChange = {},
                readOnly      = true,
                label         = { Text("Employee ID") },
                leadingIcon   = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                trailingIcon  = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Regenerate ID button
                        IconButton(onClick = {
                            employeeId = generateEmployeeId()
                            idCopied   = false
                        }) {
                            Icon(
                                imageVector        = Icons.Default.Refresh,
                                contentDescription = "Regenerate ID",
                                tint               = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Copy ID button
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(employeeId))
                            idCopied = true
                        }) {
                            Icon(
                                imageVector        = if (idCopied) Icons.Default.Check else Icons.Default.Share,
                                contentDescription = "Copy ID",
                                tint               = if (idCopied) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = OutlinedTextFieldDefaults.colors(
                    disabledTextColor        = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor      = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor       = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp
                )
            )

            // Generated Password field
            OutlinedTextField(
                value               = generatedPassword,
                onValueChange       = {},
                readOnly            = true,
                label               = { Text("Generated Password") },
                leadingIcon         = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon        = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Toggle visibility
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector        = if (passwordVisible) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                        // Regenerate password
                        IconButton(onClick = {
                            generatedPassword = generatePassword()
                            pwCopied          = false
                        }) {
                            Icon(
                                imageVector        = Icons.Default.Refresh,
                                contentDescription = "Regenerate password",
                                tint               = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Copy password
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(generatedPassword))
                            pwCopied = true
                        }) {
                            Icon(
                                imageVector        = if (pwCopied) Icons.Default.Check else Icons.Default.Share,
                                contentDescription = "Copy password",
                                tint               = if (pwCopied) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp
                )
            )

            // Password strength hint
            Surface(
                color  = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape  = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier          = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector        = Icons.Default.Info,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(16.dp)
                    )
                    Text(
                        text  = "Password includes uppercase, lowercase, numbers and special characters. Hit 🔄 to regenerate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // ── Action Buttons ──────────────────────────────────────────────
            Button(
                onClick = {
                    // Validate
                    nameError  = fullName.isBlank()
                    emailError = email.isBlank() || !email.contains("@")
                    roleError  = role.isBlank()

                    if (!nameError && !emailError && !roleError) {
                        showSuccess = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = "Add Employee",
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick  = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Cancel")
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Reusable Components ──────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(20.dp)
        )
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CredentialRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.primary
        )
    }
}
@Composable
private fun FormField(
    value:           String,
    onValueChange:   (String) -> Unit,
    label:           String,
    placeholder:     String          = "",
    isError:         Boolean         = false,
    errorMessage:    String          = "",
    leadingIcon:     ImageVector?    = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value           = value,
            onValueChange   = onValueChange,
            modifier        = Modifier.fillMaxWidth(),
            label           = { Text(label) },
            placeholder     = { Text(placeholder) },
            isError         = isError,
            leadingIcon     = leadingIcon?.let {
                { Icon(it, contentDescription = null) }
            },
            keyboardOptions = keyboardOptions,
            shape           = RoundedCornerShape(12.dp),
            singleLine      = true
        )
        if (isError && errorMessage.isNotEmpty()) {
            Text(
                text  = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}