# SimpleSVGToComposeImageVector

a simple, light lib to show path from `svg` in compose image vector

English | [简体中文](https://github.com/whiterasbk/SimpleSVGToComposeImageVector/blob/master/README-zh_cn.md)

**simple**: 
- simple usage functions provided

**light**: 
- size of compiled aar is only `19.6kb`
- only compose dependencies used, not including any other libraries

## How to use
1. add `jitpack` repositories to `settings.gradle`
   ```groovy
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories {
            google()
            mavenCentral()
            maven { url 'https://www.jitpack.io' } // add here
        }
    }
   ```
2. add dependencies to `build.gradle`
   ```groovy
   dependencies {
       implementation 'com.github.whiterasbk:SimpleSVGToComposeImageVector:$latest_version'
   }
   ```
3. using `svgToImageVector` to convert svg path into image vector
   ```kotlin
    @Composable
    fun GitHub() {
        val svg = """
    <svg
        width="16"
        height="16"
        viewBox="0 0 16 16"
        xmlns="http://www.w3.org/2000/svg">
        
        <path
            d="M8 0C3.58 0 0 3.58 0 8C0 11.54 2.29 14.53 5.47 15.59C5.87 15.66 6.02 15.42 6.02 15.21C6.02 15.02 6.01 14.39 6.01 13.72C4 14.09 3.48 13.23 3.32 12.78C3.23 12.55 2.84 11.84 2.5 11.65C2.22 11.5 1.82 11.13 2.49 11.12C3.12 11.11 3.57 11.7 3.72 11.94C4.44 13.15 5.59 12.81 6.05 12.6C6.12 12.08 6.33 11.73 6.56 11.53C4.78 11.33 2.92 10.64 2.92 7.58C2.92 6.71 3.23 5.99 3.74 5.43C3.66 5.23 3.38 4.41 3.82 3.31C3.82 3.31 4.49 3.1 6.02 4.13C6.66 3.95 7.34 3.86 8.02 3.86C8.7 3.86 9.38 3.95 10.02 4.13C11.55 3.09 12.22 3.31 12.22 3.31C12.66 4.41 12.38 5.23 12.3 5.43C12.81 5.99 13.12 6.7 13.12 7.58C13.12 10.65 11.25 11.33 9.47 11.53C9.76 11.78 10.01 12.26 10.01 13.01C10.01 14.08 10 14.94 10 15.21C10 15.42 10.15 15.67 10.55 15.59C13.71 14.53 16 11.53 16 8C16 3.58 12.42 0 8 0Z"
        />
    </svg>
        """.trimIndent()
    
        Icon(imageVector = svgToImageVector(svg), contentDescription = null, tint = Color(27, 31, 35))
    }
    
    @Preview(showBackground = true)
    @Composable
    fun GitHubPreview() {      
       GitHub()
    }
   ```

## Preview
![image](https://github.com/whiterasbk/SimpleSVGToComposeImageVector/assets/31107204/1a161d78-81c3-4900-b56b-774d13f344b1)

## How it works
for input svg string, `SimpleSVGToComposeImageVector` use `xmlpull` to parse, aiming to get following infomation

for `<svg>` tag:
- `height` attribute
- `width` attribute

for `<path>` tag:
- `d` attribute (target)

after that, `SimpleSVGToComposeImageVector` use `ImageVector.Builder.path` to generate path according to `d` attribute 

## Not supporting
`SimpleSVGToComposeImageVector` focus on `ImageVector` side, only support `ImageVector` features, so features like animation are not in develop plans. In a certain svg file, `SimpleSVGToComposeImageVector` only parse `<path>` and `<svg>` tag
