
  Pod::Spec.new do |s|
    s.name = 'CapacitorUdp'
    s.version = '0.0.1'
    s.summary = 'udp plugin'
    s.license = 'MIT'
    s.homepage = 'https://github.com/unitree-czk/capacitor-udp'
    s.author = 'Zhongkai Chen'
    s.source = { :git => 'https://github.com/unitree-czk/capacitor-udp', :tag => s.version.to_s }
    s.source_files = 'ios/Plugin/**/*.{swift,h,m,c,cc,mm,cpp}'
    s.ios.deployment_target  = '11.0'
    s.dependency 'Capacitor'
    s.dependency 'CocoaAsyncSocket'
  end