require 'sinatra'
require 'neo4j-core'
require 'neo4j/core/cypher_session/adaptors/http'

def neo4j_session
  @neo4j_session ||= Neo4j::Core::CypherSession.new(Neo4j::Core::CypherSession::Adaptors::HTTP.new('http://localhost:7474'))
end

def get_kml(from, to)
  from_latitude, from_longitude = from.split(',')
  to_latitude, to_longitude = to.split(',')

  query = """
  MATCH (start:Cell{latitude:{from_latitude}, longitude:{from_longitude}})
  with start
  MATCH (end:Cell{latitude:{to_latitude}, longitude:{to_longitude}})
  RETURN id(start), id(end)
  """

  q = neo4j_session.query(query, from_latitude: from_latitude, from_longitude: from_longitude, to_latitude: to_latitude, to_longitude: to_longitude).first

  JSON.parse(Faraday.post("http://localhost:7474/db/data/node/#{q[:'id(start)']}/path", {
    "to" => "http://localhost:7474/db/data/node/#{q[:'id(end)']}",
    "cost_property" => "cost",
    "relationships" => {
      "type" => "LINKED",
      "direction" => "out"
    },
    "algorithm" => "dijkstra"
  }).body)['nodes'].map do |node_url|
    properties = JSON.parse(Faraday.get("#{node_url}/properties").body)
    [properties['latitude'], properties['longitude']]
  end
end

get '/route.xml' do
  content_type 'text/xml'
  erb :route_xml, locals: { coordinates: get_kml(params[:from], params[:to]) }
end

get '/route.json' do
  content_type 'application/json'
  erb :route_json, locals: { coordinates: get_kml(params[:from], params[:to]) }
end

get '/route' do
  erb :route, locals: { kml_content: erb(:route_xml, locals: { coordinates: get_kml(params[:from], params[:to]) }).delete("\n") }
end
