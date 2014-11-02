# encoding: utf-8
require 'time'
require 'redis'

class Sensor
  attr_reader :id
  attr_accessor :updated_time, :rssi, :last_press_time, :name, :group, :battery
  def initialize(id)
    @id = id
  end
end

class Sensors

  def initialize conf
    @sensors = {}
  end

  def get id
    @sensors[id]
  end

  # id: string, rssi: int, status: int
  def update id, rssi, status, battery
    unless @sensors[id]
      @sensors[id] = Sensor.new(id)
    end
    @sensors[id].rssi = rssi
    @sensors[id].name = "GSS_13M_??"
    @sensors[id].group = "GSS_13M"
    @sensors[id].updated_time = Time.now
    @sensors[id].last_press_time = Time.now if status == 1
    @sensors[id].battery = battery
  end

  def clear!
    @sensors = {}
  end

  def list
    @sensors.values
  end
end

