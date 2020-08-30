#import "XunfeimscPlugin.h"
#if __has_include(<xunfeimsc/xunfeimsc-Swift.h>)
#import <xunfeimsc/xunfeimsc-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "xunfeimsc-Swift.h"
#endif

@implementation XunfeimscPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftXunfeimscPlugin registerWithRegistrar:registrar];
}
@end
