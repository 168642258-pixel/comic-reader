param(
    [string]$OutputDir = (Join-Path $PSScriptRoot ".." "build" "apk")
)

Write-Host "=== 漫画阅读 App APK 构建脚本 ===" -ForegroundColor Cyan
Write-Host ""

# 1. 检查 Java
$javaVersion = (& java -version 2>&1) -join "`n"
if ($LASTEXITCODE -ne 0 -or (-not ($javaVersion -match "version"))) {
    Write-Host "[!] 未找到 JDK。请先安装 JDK 17：" -ForegroundColor Yellow
    Write-Host "    winget install EclipseAdoptium.Temurin.17.JDK" -ForegroundColor White
    Write-Host "    或从 https://adoptium.net/ 下载安装" -ForegroundColor White
    exit 1
}
Write-Host "[OK] Java 已安装" -ForegroundColor Green

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

# 2. 检查 ANDROID_HOME
$androidHome = $env:ANDROID_HOME
if (-not $androidHome) {
    $androidHome = $env:ANDROID_SDK_ROOT
}
if (-not $androidHome) {
    # 常见默认路径
    $possiblePaths = @(
        Join-Path $env:LOCALAPPDATA "Android" "Sdk",
        "C:\Android\Sdk",
        "C:\Program Files\Android\Sdk"
    )
    foreach ($p in $possiblePaths) {
        if (Test-Path $p) {
            $androidHome = $p
            break
        }
    }
}

if (-not $androidHome -or -not (Test-Path $androidHome)) {
    Write-Host "[!] 未找到 Android SDK。正在安装命令行工具..." -ForegroundColor Yellow
    
    $sdkPath = Join-Path $env:USERPROFILE "Android" "Sdk"
    New-Item -ItemType Directory -Force -Path $sdkPath | Out-Null
    
    # 下载 Android 命令行工具
    $cmdlineToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
    $toolsZip = Join-Path $env:TEMP "cmdline-tools.zip"
    
    try {
        Write-Host "    下载命令行工具..." -ForegroundColor Yellow
        Invoke-WebRequest -Uri $cmdlineToolsUrl -OutFile $toolsZip -UseBasicParsing
        
        Write-Host "    解压..." -ForegroundColor Yellow
        Expand-Archive -Path $toolsZip -DestinationPath (Join-Path $sdkPath "temp") -Force
        New-Item -ItemType Directory -Force -Path (Join-Path $sdkPath "cmdline-tools") | Out-Null
        Move-Item -Path (Join-Path $sdkPath "temp" "cmdline-tools" "*") -Destination (Join-Path $sdkPath "cmdline-tools" "latest") -Force
        Remove-Item -Path (Join-Path $sdkPath "temp") -Recurse -Force
        Remove-Item $toolsZip -Force
        
        # 设置环境变量
        $env:ANDROID_HOME = $sdkPath
        [Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdkPath, "User")
        $androidHome = $sdkPath
        
        # 安装需要的 SDK 组件
        $sdkManager = Join-Path $androidHome "cmdline-tools" "latest" "bin" "sdkmanager.bat"
        Write-Host "    安装 Android SDK 34 和构建工具..." -ForegroundColor Yellow
        & $sdkManager --sdk_root=$androidHome "platforms;android-34" "build-tools;34.0.0" | Out-Null
    } catch {
        Write-Host "[!] 下载失败，请手动安装 Android SDK:" -ForegroundColor Red
        Write-Host "    1. 下载 https://developer.android.com/studio#command-line-tools-only" -ForegroundColor White
        Write-Host "    2. 解压到 $sdkPath\cmdline-tools\latest" -ForegroundColor White
        Write-Host "    3. 运行: sdkmanager 'platforms;android-34' 'build-tools;34.0.0'" -ForegroundColor White
        Write-Host "    4. 设置环境变量 ANDROID_HOME=$sdkPath" -ForegroundColor White
        exit 1
    }
}

Write-Host "[OK] Android SDK: $androidHome" -ForegroundColor Green
$env:ANDROID_HOME = $androidHome

# 3. 创建 local.properties
$localProps = Join-Path $projectRoot "local.properties"
"sdk.dir=$($androidHome -replace '\\', '/')" | Set-Content $localProps
Write-Host "[OK] local.properties 已创建" -ForegroundColor Green

# 4. 生成 Gradle Wrapper（如没有）
$gradlew = Join-Path $projectRoot "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    Write-Host "    生成 Gradle Wrapper..." -ForegroundColor Yellow
    $gradleVersion = "8.5"
    $gradleUrl = "https://services.gradle.org/distributions/gradle-${gradleVersion}-bin.zip"
    $gradleZip = Join-Path $env:TEMP "gradle.zip"
    
    try {
        Invoke-WebRequest -Uri $gradleUrl -OutFile $gradleZip -UseBasicParsing
        Expand-Archive -Path $gradleZip -DestinationPath (Join-Path $env:TEMP "gradle-extract") -Force
        $gradleBin = Join-Path $env:TEMP "gradle-extract" "gradle-${gradleVersion}" "bin" "gradle.bat"
        & $gradleBin wrapper --gradle-version=$gradleVersion
        Remove-Item $gradleZip -Force
        Remove-Item (Join-Path $env:TEMP "gradle-extract") -Recurse -Force
    } catch {
        Write-Host "[!] Gradle Wrapper 生成失败，请手动运行: gradle wrapper" -ForegroundColor Yellow
        exit 1
    }
}
Write-Host "[OK] Gradle Wrapper 就绪" -ForegroundColor Green

# 5. 构建 APK
Write-Host ""
Write-Host "=== 开始构建 Debug APK ===" -ForegroundColor Cyan
& $gradlew assembleDebug

if ($LASTEXITCODE -eq 0) {
    # 复制 APK 到输出目录
    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
    $apkPath = Join-Path $projectRoot "app" "build" "outputs" "apk" "debug" "app-debug.apk"
    if (Test-Path $apkPath) {
        Copy-Item $apkPath (Join-Path $OutputDir "ComicReader-debug.apk") -Force
        Write-Host ""
        Write-Host "=== 构建成功! ===" -ForegroundColor Green
        Write-Host "APK 位置: $(Join-Path $OutputDir 'ComicReader-debug.apk')" -ForegroundColor Cyan
    }
} else {
    Write-Host "[!] 构建失败，请检查错误信息" -ForegroundColor Red
    exit 1
}
