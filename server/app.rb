#!/usr/bin/ruby -Ku
# encoding: utf-8

require 'sinatra/base'
require 'json'
require_relative 'sensors'

class ApiServer < Sinatra::Base

  configure do
    enable :logging
  end

  before do
    allow_origin = ".dwango.co.jp"
    if request.env['HTTP_ORIGIN'] && (request.env['HTTP_ORIGIN'].sub(/\/$/,'').end_with?(allow_origin) || request.env['HTTP_ORIGIN'] == "null")
      headers 'Access-Control-Allow-Origin' => request.env['HTTP_ORIGIN'],
              'Access-Control-Allow-Methods' => ['OPTIONS', 'GET', 'POST']
    end
  end

  def logger
    env['app.logger'] || env['rack.logger']
  end

  def sensor_res s
    {
      id: s.id,
      type: "door",
      group: s.group,
      name: s.name,
      rssi: s.rssi,
      battery: s.battery,
      state: (s.last_press_time && s.last_press_time + 15 > Time.now) ? 1 : 0,
      updated: s.updated_time.to_i
    }
  end

  get '/status' do
    content_type :json
    {:status => 'ok'}.to_json
  end

  get '/sensors/all' do
    content_type :json
    {:status => 'ok', :time => Time.now.to_i, :sensors => settings.sensors.list.map{|s|sensor_res(s)} }.to_json
  end

  get '/sensors/clear' do
    if params[:key] != settings.key
      halt 403, {:status => 'error', :message => 'invalid api_key'}.to_json
    end
    settings.sensors.clear!
    {:status => 'ok'}.to_json
  end

  get '/sensors/:id' do
    content_type :json
    s = settings.sensors.get(params[:id])
    halt 404, {:status => 'error', :message => 'sensor not found'}.to_json  unless s

    {:status => 'ok', :sensor => sensor_res(s) }.to_json
  end

  post '/sensors/:id' do
    content_type :json

    if params[:key] != settings.key
      halt 403, {:status => 'error', :message => 'invalid api_key'}.to_json
    end

    logger.info([params[:id], params[:rssi].to_i, params[:status].to_i, params[:battery].to_i].join(','))

    settings.sensors.update(params[:id], params[:rssi].to_i, params[:status].to_i, params[:battery].to_i)

    {:status => 'ok', :sensors => settings.sensors.list }.to_json
  end

end


conf = if File.exist?("conf/app.json")
  open("conf/app.json") {|f|
    JSON.parse(f.read)
  }
else
  {}
end

# web
ApiServer.set :sensors, Sensors.new(conf)
ApiServer.set :key, conf["update_key"]

ApiServer.run! :host => 'localhost', :port => (ARGV[0] || conf["port"] || 4567)

