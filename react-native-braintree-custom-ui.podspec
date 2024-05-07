require 'json'
package = JSON.parse(File.read('./package.json'))

Pod::Spec.new do |s|
  s.name             = "RCTBraintree"
  s.version          = package["version"]
  s.summary          = package["description"]
  s.requires_arc     = true
  s.license          = { :type => package["license"] }
  s.homepage         = package["homepage"]
  s.authors          = { package["author"]["name"] => package["author"]["email"] }
  s.source           = { :git => s.homepage, :tag => "v#{s.version}" }
  s.platform         = :ios, "12.0"
  s.source_files     = 'ios/RCTBraintree/**/*.{h,m}'
  s.dependency         'React'
  s.dependency 'Braintree', '6.18.1'
  s.dependency 'Braintree/DataCollector', '6.18.1'
  s.dependency 'Braintree/LocalPayment', '6.18.1'
end
