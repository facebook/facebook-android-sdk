android_build_config(
  name = 'build_config',
  package = 'com.facebook',
)

android_resource(
  name = 'res',
  res = 'res',
  package = 'com.facebook',
  deps = [
  ],
)

android_library(
  name = 'android-sdk',
  srcs = glob(['src/**/*.java']),
  deps = [
    ':build_config',
    ':res',
    '//libs:android-support-v4',
    '//libs:bolts',
  ],
  visibility = [
    'PUBLIC',
  ],
)

project_config(
  src_target = ':android-sdk',
  src_roots = ['src'],
)
