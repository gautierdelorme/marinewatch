require 'sinatra'
require 'neo4j-core'
require 'neo4j/core/cypher_session/adaptors/http'

def neo4j_session
  @neo4j_session ||= Neo4j::Core::CypherSession.new(Neo4j::Core::CypherSession::Adaptors::HTTP.new('http://localhost:7474'))
end

get '/' do
  content_type 'text/xml'

  from_latitude, from_longitude = params[:from].split(',')
  to_latitude, to_longitude = params[:to].split(',')

  query = """
  MATCH (start:Cell{latitude:{from_latitude}, longitude:{from_longitude}})
  with start
  MATCH (end:Cell{latitude:{to_latitude}, longitude:{to_longitude}})
  RETURN id(start), id(end)
  """

  q = neo4j_session.query(query, from_latitude: from_latitude, from_longitude: from_longitude, to_latitude: to_latitude, to_longitude: to_longitude).first

  result = JSON.parse(Faraday.post("http://localhost:7474/db/data/node/#{q[:'id(start)']}/path", {
    "to" => "http://localhost:7474/db/data/node/#{q[:'id(end)']}",
    "cost_property" => "cost",
    "relationships" => {
      "type" => "LINKED",
      "direction" => "out"
    },
    "algorithm" => "dijkstra"
  }).body)

  erb :index, locals: { coordinates: result['nodes'].map do |node_url|
    properties = JSON.parse(Faraday.get("#{node_url}/properties").body)
    [properties['latitude'], properties['longitude']]
  end }
end
