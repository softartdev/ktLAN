# Contributing to ktLAN

Thank you for your interest in contributing to ktLAN! This document provides guidelines and best practices for contributing to this Kotlin Multiplatform project.

## Table of Contents

- [Project Overview](#project-overview)
- [Development Setup](#development-setup)
- [Code Style and Standards](#code-style-and-standards)
- [Architecture Guidelines](#architecture-guidelines)
- [Navigation Best Practices](#navigation-best-practices)
- [Testing Guidelines](#testing-guidelines)
- [Pull Request Process](#pull-request-process)
- [Common Issues and Solutions](#common-issues-and-solutions)

## Project Overview

ktLAN is a Kotlin Multiplatform application that provides LAN communication capabilities across multiple platforms (Android, iOS, Desktop, Web). The project uses:

- **Kotlin Multiplatform** for cross-platform code sharing
- **Compose Multiplatform** for UI
- **Koin** for dependency injection
- **Navigation Compose** for navigation
- **Ktor** for networking
- **WebRTC** for peer-to-peer communication

## Development Setup

### Prerequisites

- JDK 17 or higher
- Android Studio or IntelliJ IDEA
- Xcode (for iOS development)
- Gradle 8.0+

### Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/ktLAN.git
   cd ktLAN
   ```

2. Open the project in Android Studio or IntelliJ IDEA

3. Sync Gradle files and wait for dependencies to download

4. Run the application:
   ```bash
   # Desktop
   ./gradlew :composeApp:desktopRun
   
   # Android
   ./gradlew :composeApp:assembleDebug
   
   # iOS (requires macOS)
   ./gradlew :composeApp:iosSimulatorArm64Test
   
   # Web (wasmJS)
   ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
   ```

## Code Style and Standards

### Kotlin Style

- Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use 4 spaces for indentation
- Use meaningful variable and function names
- Prefer `val` over `var` when possible
- Use expression bodies for simple functions
- **Use explicit type annotations** when they improve readability
- **Prefer direct operations** over unnecessary abstractions
- **Keep imports clean and organized** - remove unused imports regularly

### File Organization

```
src/
├── commonMain/
│   ├── kotlin/
│   │   └── com/softartdev/ktlan/
│   │       ├── presentation/          # ViewModels and UI state
│   │       ├── domain/                # Business logic and models
│   │       └── data/                  # Repositories and data sources
│   └── resources/
├── androidMain/
├── iosMain/
├── desktopMain/
└── wasmJsMain/
```

### Naming Conventions

- **Classes**: PascalCase (e.g., `NetworksViewModel`)
- **Functions**: camelCase (e.g., `onAction`)
- **Variables**: camelCase (e.g., `selectedTab`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`)
- **Files**: PascalCase for classes, camelCase for functions (e.g., `MainBottomNavScreen.kt`)

## Architecture Guidelines

### MVVM Pattern

The project follows the MVVM (Model-View-ViewModel) pattern:

- **View**: Compose UI components
- **ViewModel**: Business logic and state management
- **Model**: Data classes and domain entities

### Dependency Injection

Use Koin for dependency injection:

```kotlin
// Define modules
val uiModules = module {
    singleOf<Router>(::RouterImpl)
    factoryOf(::NetworksViewModel)
}

// Inject dependencies
class NetworksViewModel(
    private val repo: NetworksRepo,
    private val router: Router
) : ViewModel()

// ✅ Good: Keep dependencies minimal and focused
// ❌ Avoid: Injecting unnecessary dependencies like CoroutineDispatchers
```

### State Management

- Use `StateFlow` for reactive state management
- Keep ViewModels focused on a single responsibility
- Use sealed classes for UI states
- **Avoid unnecessary state** - Only store what's actually needed

```kotlin
sealed interface NetworksAction {
    data object Refresh : NetworksAction
    data class UseAsBindHost(val address: String) : NetworksAction
}

data class NetworksResult(
    val loading: Boolean = false,
    val interfaces: List<NetworkInterfaceInfo> = emptyList(),
    val error: String? = null
    // ✅ Good: Only essential state
    // ❌ Avoid: selectedIp, temporary UI state, etc.
)
```

### Repository-Based Architecture for Long-Running Operations

For operations that should survive ViewModel recreation (like network scans, file operations, etc.), use **Use Case (Interactor)** architecture following Clean Architecture principles:

```kotlin
// ✅ Good: Use Case manages long-running operations
sealed interface ScanState {
    data object Idle : ScanState
    data object Loading : ScanState
    data class Success(val hosts: List<HostModel>) : ScanState
    data class Error(val message: String) : ScanState
}

open class ScanUseCase(
    private val scanRepo: ScanRepo
) {
    // Long-lived scope for operations that survive ViewModel recreation
    private val scanScope = CoroutineScope(SupervisorJob())
    
    // State management for scan operations
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    open val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    
    // Current scan job to allow cancellation
    private var currentScanJob: Job? = null

    open fun startScan(startIp: String, endIp: String, ports: List<Int>) {
        currentScanJob?.cancel()
        currentScanJob = scanScope.launch {
            try {
                _scanState.value = ScanState.Loading
                val hosts = scanRepo.scanRangeParallel(startIp, endIp, ports)
                _scanState.value = ScanState.Success(hosts)
            } catch (error: Throwable) {
                _scanState.value = ScanState.Error(error.message ?: "Unknown error")
            }
        }
    }
}

// ✅ Good: Repository is stateless (pure data access)
open class ScanRepo {
    open suspend fun scanRangeParallel(
        coroutineContext: CoroutineContext,
        startIp: String,
        endIp: String,
        ports: List<Int>
    ): List<HostModel> {
        // Pure data operations, no state management
    }
}

// ✅ Good: ViewModel combines use case state with local UI state
class ScanViewModel(
    private val scanUseCase: ScanUseCase,
    private val router: Router,
    private val navParameters: AppNavGraph.BottomTab.Scan
) : ViewModel() {
    
    // Local UI state (input fields)
    private val _uiState = MutableStateFlow(ScanResult.Success())
    
    // Combined state from use case and local UI state
    private val _combinedState = MutableStateFlow<ScanResult>(ScanResult.Success())
    val stateFlow: StateFlow<ScanResult> = _combinedState

    init {
        // Combine use case state with local UI state
        combine(
            scanUseCase.scanState,
            _uiState
        ) { useCaseState, uiState ->
            when (useCaseState) {
                is ScanState.Idle -> uiState
                is ScanState.Loading -> ScanResult.Loading
                is ScanState.Success -> uiState.copy(hosts = useCaseState.hosts)
                is ScanState.Error -> ScanResult.Error(useCaseState.message)
            }
        }.onEach { combinedState ->
            _combinedState.value = combinedState
        }.launchIn(viewModelScope)
    }
}

// ✅ Good: Dependency injection with proper layering
val domainModule: Module = module {
    // Repositories (stateless data access layer)
    factoryOf(::ScanRepo)
    singleOf(::ConnectRepo)
    singleOf(::SocketRepo)
    singleOf(::NetworksRepo)
    
    // Use Cases (business logic layer with state management)
    singleOf(::ScanUseCase)
}
```

### Benefits of Use Case Architecture

1. **Separation of Concerns**: 
   - **Repository**: Pure data access (stateless)
   - **Use Case**: Business logic and state management
   - **ViewModel**: UI logic and state combination

2. **State Persistence**: Long-running operations continue even when ViewModels are recreated

3. **Testability**: Easy to mock and test each layer independently

4. **Resource Management**: Proper cancellation and cleanup of operations

5. **Reusability**: Use Cases can be shared across multiple ViewModels

6. **Clean Architecture**: Follows SOLID principles and dependency inversion

## Navigation Best Practices

### Navigation Structure

The project uses a hierarchical navigation structure:

```kotlin
sealed interface AppNavGraph {
    sealed interface BottomTab : AppNavGraph {
        data object Connect : BottomTab
        data object Scan : BottomTab
        data object Networks : BottomTab
        data class Socket(val remoteHost: String? = null) : BottomTab
        data object Settings : BottomTab
    }
}
```

### Navigation Implementation

1. **Use Router for Navigation**: Always use the Router interface in ViewModel's for navigation operations
2. **Automatic State Tracking**: Use `currentBackStackEntryAsState()` for bottom navigation state
3. **Parameter-Aware Navigation**: Handle navigation with parameters by creating fresh ViewModel instances
4. **Route Comparison**: Use class names for route matching:

```kotlin
// ✅ Good: Automatic state tracking
val navBackStackEntry by navController.currentBackStackEntryAsState()

NavigationBarItem(
    selected = navBackStackEntry?.destination?.route
        ?.contains(bottomTab::class.simpleName.orEmpty()) ?: false,
    onClick = { router.bottomNavigate(bottomTab) }
)

// ✅ Good: Parameter-aware navigation in Router
override fun <T : Any> bottomNavigate(route: T) = bottomNavController!!.navigate(route) {
    popUpTo(bottomNavController!!.graph.findStartDestination().id) {
        saveState = true
    }
    
    val hasParameters = when (route) {
        is AppNavGraph.BottomTab.Scan -> 
            route.startIp != null || route.endIp != null
        is AppNavGraph.BottomTab.Socket -> 
            route.remoteHost != null || route.bindHost != null
        else -> false
    }
    
    if (hasParameters) {
        // For routes with parameters, create fresh ViewModel
        launchSingleTop = false
        popUpTo(bottomNavController!!.graph.findStartDestination().id) {
            inclusive = true
            saveState = false
        }
    } else {
        // For routes without parameters, reuse existing instance
        launchSingleTop = true
        restoreState = true
    }
}

// ❌ Avoid: Manual state management
var selectedTab by remember { mutableStateOf(startBottomTab) }
```

### Navigation Actions

When implementing navigation from ViewModels:

```kotlin
// ✅ Good: Use Router for navigation with parameters
fun onAction(action: NetworksAction) {
    when (action) {
        is NetworksAction.UseAsBindHost -> {
            router.bottomNavigate(AppNavGraph.BottomTab.Socket(bindHost = action.address))
        }
    }
}

// ✅ Good: Handle multiple navigation parameters
fun launch() = viewModelScope.launch {
    navParameters.bindHost?.let { bindHost ->
        state.update { it.copy(bindHost = bindHost) }
    }
    navParameters.remoteHost?.let { remoteHost ->
        state.update { it.copy(remoteHost = remoteHost) }
    }
}
```

## Testing Guidelines

### Test Structure

Follow this test organization:

```
src/
├── commonTest/
├── androidTest/
├── desktopTest/
└── iosTest/
```

### Writing Tests

1. **Test Naming**: Use descriptive test names that explain the scenario
2. **Test Structure**: Follow the Arrange-Act-Assert pattern
3. **Mocking**: Use fake implementations for dependencies
4. **Screenshot Capture**: Use screenshots for debugging complex UI tests

#### Unit Tests

```kotlin
@Test
fun testNetworksToSocketNavigation() = runTest {
    // Arrange
    val routerStub = RouterStub()
    val viewModel = NetworksViewModel(fakeRepo, routerStub)
    
    // Act
    viewModel.onAction(NetworksAction.UseAsBindHost("192.168.1.1"))
    
    // Assert
    assertEquals(
        expected = AppNavGraph.BottomTab.Socket(bindHost = "192.168.1.1"),
        actual = routerStub.lastBottomRoute
    )
}
```

#### Integration Tests

For testing navigation flows with actual Router injection:

```kotlin
@Test
fun testNavigationWithRouter() = runTest {
    val router: Router by KoinJavaComponent.inject(Router::class.java)
    
    // Test programmatic navigation
    router.bottomNavigate(AppNavGraph.BottomTab.Socket(remoteHost = "1.2.3.4"))
    composeTestRule.waitForIdle()
    
    // Verify the navigation worked and parameters were passed
    composeTestRule.onNodeWithText("1.2.3.4").assertExists()
}
```

### UI Testing

For Compose UI tests:

```kotlin
@Test
fun testNavigationBetweenTabs() = runTest {
    composeTestRule.onNodeWithText("Networks").performClick()
    composeTestRule.waitForIdle()
    
    composeTestRule.onNodeWithText("LAN Chat").performClick()
    composeTestRule.waitForIdle()
    
    composeTestRule.onNodeWithText("LAN Chat").assertExists()
}
```

### UI Component Best Practices

When implementing UI components with actions:

```kotlin
// ✅ Good: Direct clipboard operations
val clipboard: ClipboardManager = LocalClipboardManager.current
Button(
    onClick = { clipboard.setText(AnnotatedString(ip)) },
    content = { Text(text = stringResource(Res.string.networks_copy)) }
)

// ✅ Good: Direct action handling
Button(
    onClick = { onAction(ScanAction.UseAsRemoteHost(result.ip)) },
    content = { Text(text = stringResource(Res.string.networks_use)) }
)

// ❌ Avoid: Unnecessary state updates for UI-only operations
Button(onClick = { 
    onAction(SomeAction.Copy(ip)) // Don't create actions for simple UI operations
})
```

### Import Organization

Keep imports clean and well-organized:

```kotlin
// ✅ Good: Clean, organized imports
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager

// ❌ Avoid: Unused imports and poor organization
import androidx.compose.material3.Scaffold  // Unused import
import androidx.compose.runtime.State       // Unused import
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
```

### Advanced UI Testing with Screenshots

For complex navigation testing, use screenshot capture for debugging:

```kotlin
@Test
fun testNetworksToSocketNavigationViaUseButton() = runTest {
    val router: Router by KoinJavaComponent.inject(Router::class.java)

    composeTestRule.onNodeWithText("Networks").performClick()
    composeTestRule.waitForIdle()

    captureScreenshot(composeTestRule, "networks_tab.png")

    router.bottomNavigate(AppNavGraph.BottomTab.Socket(remoteHost = "1.2.3.4"))
    composeTestRule.waitForIdle()

    captureScreenshot(composeTestRule, "socket_tab.png")
    
    composeTestRule.onNodeWithText("1.2.3.4").assertExists()
}

/**
 * Captures a screenshot of the entire composable content and saves it to a file.
 */
fun captureScreenshot(rule: ComposeTestRule, filename: String) {
    val imageBitmap: ImageBitmap = rule.onRoot().captureToImage()
    val bufferedImage: BufferedImage = imageBitmap.toAwtImage()

    val outputDir = File("build/test-screenshots")
    if (!outputDir.exists()) outputDir.mkdirs()
    val file = File(outputDir, filename)

    ImageIO.write(bufferedImage, "PNG", file)
    Napier.d("Screenshot saved to ${file.absolutePath}")
}
```

### Testing Navigation State Management

Test navigation state updates when navigating to already opened tabs with parameters:

```kotlin
@Test
fun testScanTabStateNotUpdatedWhenAlreadyOpen() = runTest {
    val router: Router by KoinJavaComponent.inject(Router::class.java)

    // First, navigate to Scan tab (should have default values)
    composeTestRule.onNodeWithText("Scan").performClick()
    composeTestRule.waitForIdle()

    // Verify default values are present
    composeTestRule.onNodeWithText("192.168.0.100").assertExists() // Default start IP
    composeTestRule.onNodeWithText("192.168.0.125").assertExists() // Default end IP

    // Navigate to Networks tab
    composeTestRule.onNodeWithText("Networks").performClick()
    composeTestRule.waitForIdle()

    // Use "Scan" button to navigate back to Scan with new parameters
    // This should update the TextFields but currently doesn't
    router.bottomNavigate(AppNavGraph.BottomTab.Scan(startIp = "10.0.0.1", endIp = "10.0.0.255"))
    composeTestRule.waitForIdle()

    // This test should fail - the TextFields should show new values but they don't
    composeTestRule.onNodeWithText("10.0.0.1").assertExists() // Should be new start IP
    composeTestRule.onNodeWithText("10.0.0.255").assertExists() // Should be new end IP
}
```

### Testing Repository-Based Architecture

Test that long-running operations survive ViewModel recreation:

```kotlin
@Test
fun testScanOperationSurvivesViewModelRecreation() = runTest {
    val router: Router by KoinJavaComponent.inject(Router::class.java)

    // Start a scan operation
    composeTestRule.onNodeWithText("Scan").performClick()
    composeTestRule.waitForIdle()
    
    composeTestRule.onNodeWithText("Scan").performClick() // Start scan
    composeTestRule.waitForIdle()

    // Navigate away and back - scan should continue
    composeTestRule.onNodeWithText("Networks").performClick()
    composeTestRule.waitForIdle()
    
    composeTestRule.onNodeWithText("Scan").performClick()
    composeTestRule.waitForIdle()

    // Scan should still be running or completed
    composeTestRule.onNodeWithText("Loading").assertExists() // Or scan results
}
```

### Testing Use Case Architecture

Test ViewModel recreation scenarios with comprehensive unit tests that work across all platforms:

```kotlin
@Test
fun `test scan operation survives ViewModel recreation`() = runTest {
    // Arrange
    val viewModel1 = ScanViewModel(
        scanUseCase = scanUseCaseStub,
        router = routerStub,
        navParameters = navParameters
    )

    // Act - Start scan in first ViewModel
    viewModel1.onAction(ScanAction.LaunchScan)

    // Verify scan is running
    assertTrue(scanUseCaseStub.isScanRunning)

    // Create new ViewModel (simulating recreation)
    val viewModel2 = ScanViewModel(
        scanUseCase = scanUseCaseStub, // Same use case instance
        router = routerStub,
        navParameters = navParameters
    )

    // Assert - Scan should still be running
    assertTrue(scanUseCaseStub.isScanRunning)
    assertEquals(ScanResult.Loading, viewModel2.stateFlow.value)
}

@Test
fun `test scan results persist after ViewModel recreation`() = runTest {
    // Arrange
    val testHosts = listOf(
        HostModel("192.168.1.1", listOf(22, 80)),
        HostModel("192.168.1.2", listOf(443))
    )

    val viewModel1 = ScanViewModel(
        scanUseCase = scanUseCaseStub,
        router = routerStub,
        navParameters = navParameters
    )

    // Act - Start scan and complete it
    viewModel1.onAction(ScanAction.LaunchScan)
    scanUseCaseStub.completeScanWithSuccess(testHosts)

    // Create new ViewModel (simulating recreation)
    val viewModel2 = ScanViewModel(
        scanUseCase = scanUseCaseStub,
        router = routerStub,
        navParameters = navParameters
    )

    // Assert - Use case should have the results
    assertEquals(ScanState.Success(testHosts), scanUseCaseStub.scanState.value)
    assertEquals(ScanResult.Success(), viewModel2.stateFlow.value)
}
```

### Cross-Platform Testing Best Practices

1. **Use StateFlow.value**: Access state directly instead of waiting for Flow emissions
2. **Test Use Case State**: Verify state persistence at the Use Case level rather than ViewModel level
3. **Comprehensive Assertions**: Test both Use Case state and ViewModel combined state
4. **Platform-Agnostic**: Write tests that work consistently across Android, iOS, Desktop, and Web
5. **Stub Naming**: Use descriptive names like `ScanUseCaseStub` instead of `FakeScanUseCase`
6. **Separate Stub Files**: Keep stub implementations in separate files for better organization

### Testing Best Practices

1. **Test Use Case State**: Verify operations survive ViewModel recreation at the Use Case level
2. **Use Stub Implementations**: Create stub implementations for isolated testing
3. **Test State Persistence**: Ensure long-running operations continue across ViewModel recreations
4. **Test Error Scenarios**: Verify error handling and state recovery
5. **Cross-Platform Compatibility**: Write tests that work on all target platforms
6. **Comprehensive Assertions**: Test both Use Case state and ViewModel combined state
7. **Stub Organization**: Keep stub implementations in separate files with descriptive naming

### Stub Implementations for Testing

Create proper stub implementations for testing in separate files:

```kotlin
// ✅ Good: Stub Use Case for testing (in separate file)
class ScanUseCaseStub : ScanUseCase(ScanRepoStub()) {
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    override val scanState: StateFlow<ScanState> = _scanState

    var isScanRunning: Boolean = false
        private set

    override fun startScan(startIp: String, endIp: String, ports: List<Int>) {
        isScanRunning = true
        _scanState.value = ScanState.Loading
    }

    override fun cancelScan() {
        isScanRunning = false
        _scanState.value = ScanState.Idle
    }

    override fun resetScan() {
        isScanRunning = false
        _scanState.value = ScanState.Idle
    }

    fun completeScanWithSuccess(hosts: List<HostModel>) {
        isScanRunning = false
        _scanState.value = ScanState.Success(hosts)
    }

    fun completeScanWithError(message: String) {
        isScanRunning = false
        _scanState.value = ScanState.Error(message)
    }
}

// ✅ Good: Stub Repository for testing (in separate file)
class ScanRepoStub : ScanRepo() {
    override suspend fun scanRangeParallel(
        coroutineContext: CoroutineContext,
        startIp: String,
        endIp: String,
        ports: List<Int>
    ): List<HostModel> {
        return emptyList() // Simplified for testing
    }
}

// ✅ Good: Stub Router for testing (in separate file)
class RouterStub : Router {
    var lastBottomRoute: Any? = null

    override fun setController(navController: Any) {}
    override fun setBottomNavController(navController: Any) {}
    override fun <T : Any> navigate(route: T) {}
    override fun <T : Any> navigateClearingBackStack(route: T) {}
    override fun <T : Any> popBackStack(route: T, inclusive: Boolean, saveState: Boolean): Boolean = false
    override fun popBackStack(): Boolean = false
    override fun <T : Any> bottomNavigate(route: T) {
        lastBottomRoute = route
    }
    override fun releaseController() {}
    override fun releaseBottomNavController() {}
}
```

### Test Setup

For UI tests, use proper setup with Koin and lifecycle management:

```kotlin
@Before
fun setUp() {
    Napier.base(antilog = DebugAntilog())
    when (GlobalContext.getKoinApplicationOrNull()) {
        null -> startKoin {
            printLogger(level = Level.DEBUG)
            modules(sharedModules + uiTestModules)
        }
        else -> loadKoinModules(sharedModules + uiTestModules)
    }
    val lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Swing)
    composeTestRule.setContent {
        CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
            App()
        }
    }
    composeTestRule.waitForIdle()
}

@After
fun tearDown() {
    unloadKoinModules(sharedModules + uiTestModules)
    Napier.takeLogarithm()
}
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew :composeApp:desktopTest --tests "*NavigationTest*"

# Run tests with coverage
./gradlew :composeApp:desktopTest --tests "*" --info

# Run tests and generate screenshots
./gradlew :composeApp:desktopTest --tests "*NavigationTest*"
```

## Pull Request Process

### Before Submitting

1. **Run Tests**: Ensure all tests pass
2. **Code Style**: Follow the project's coding standards
3. **Documentation**: Update documentation if needed
4. **Self-Review**: Review your changes before submitting

### Pull Request Template

```markdown
## Description
Brief description of the changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] UI tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No breaking changes (or breaking changes documented)
```

## Common Issues and Solutions

### Navigation Issues

**Problem**: Bottom navigation not updating after programmatic navigation
**Solution**: Use `currentBackStackEntryAsState()` instead of manual state management

**Problem**: Route comparison not working with parameters
**Solution**: Use class name comparison: `route.contains(bottomTab::class.simpleName)`

**Problem**: Navigation parameters not being handled correctly
**Solution**: Use explicit parameter handling in ViewModel's `launch()` method

### Code Organization Issues

**Problem**: Unnecessary actions for simple UI operations
**Solution**: Use direct operations (like clipboard) instead of creating actions

**Problem**: ViewModels with too many dependencies
**Solution**: Keep dependencies minimal and focused on core functionality

**Problem**: Unnecessary state in data classes
**Solution**: Only store essential state, avoid temporary UI state in data classes

**Problem**: Unused imports cluttering the code
**Solution**: Regularly clean up imports and remove unused ones

**Problem**: Import organization making code harder to read
**Solution**: Group imports logically and maintain consistent ordering

### Compose Issues

**Problem**: UI not updating after state changes
**Solution**: Ensure state is properly collected with `collectAsState()`

**Problem**: Compose previews not working
**Solution**: Use `@Preview` annotation and provide sample data

### Multiplatform Issues

**Problem**: Platform-specific code not compiling
**Solution**: Use `expect/actual` declarations for platform-specific implementations

**Problem**: Dependencies not available on all platforms
**Solution**: Check platform compatibility and use `expect/actual` for platform-specific dependencies

### Testing Issues

**Problem**: UI tests failing with multiple matching nodes
**Solution**: Use more specific selectors or `onAllNodesWithText()` to handle multiple elements

**Problem**: Navigation tests not working as expected
**Solution**: Use screenshot capture to debug UI state and verify navigation parameters

**Problem**: Tests timing out or flaky
**Solution**: Add proper `waitForIdle()` calls and use `TestLifecycleOwner` for coroutine management

**Problem**: Compilation errors due to unused imports
**Solution**: Regularly run code cleanup tools and remove unused imports

**Problem**: Import conflicts or ambiguous references
**Solution**: Use explicit imports and avoid wildcard imports

**Problem**: TextFields not updating when navigating to already opened tab with new parameters
**Solution**: Modify Router implementation to handle parameter changes by clearing back stack and creating fresh ViewModel instances

**Problem**: ViewModel state not refreshing when navigating with different parameters
**Solution**: Use conditional navigation logic based on parameter presence

**Problem**: Long-running operations interrupted when ViewModel is recreated
**Solution**: Move operations to repositories with long-lived scopes and state management

**Problem**: Repository state not persisting across ViewModel recreations
**Solution**: Use singleton repositories and StateFlow for state management

**Problem**: Complex state management between repository and ViewModel
**Solution**: Use Flow.combine to merge repository state with local UI state

### Testing Best Practices

1. **Use StateFlow.value**: Access state directly instead of waiting for Flow emissions
2. **Test ViewModel Recreation**: Verify operations survive ViewModel recreation
3. **Use Stub Implementations**: Create stub implementations for isolated testing
4. **Test State Persistence**: Ensure long-running operations continue across ViewModel recreations
5. **Test Error Scenarios**: Verify error handling and state recovery
6. **Comprehensive Assertions**: Test both Use Case state and ViewModel combined state
7. **Separate Stub Files**: Keep stub implementations in separate files for better organization

### Common Issues and Solutions

**Problem**: Long-running operations interrupted when ViewModel is recreated
**Solution**: Move operations to Use Cases with long-lived scopes and state management

**Problem**: Repository state not persisting across ViewModel recreations
**Solution**: Use singleton Use Cases and StateFlow for state management

**Problem**: Complex state management between Use Case and ViewModel
**Solution**: Use Flow.combine to merge Use Case state with local UI state

**Problem**: Testing ViewModel recreation scenarios
**Solution**: Create stub Use Cases and test state persistence across ViewModel instances

**Problem**: Operations not surviving navigation
**Solution**: Use Use Cases as singletons in dependency injection

## Getting Help

- **Issues**: Create an issue on GitHub
- **Discussions**: Use GitHub Discussions for questions
- **Documentation**: Check the project documentation
- **Code Examples**: Look at existing code for patterns

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Help others learn and grow
- Follow the project's coding standards

Thank you for contributing to ktLAN! 🚀
