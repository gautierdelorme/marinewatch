require 'sinatra'
require 'neo4j-core'
require 'neo4j/core/cypher_session/adaptors/http'

def neo4j_session
  @neo4j_session ||= Neo4j::Core::CypherSession.new(Neo4j::Core::CypherSession::Adaptors::HTTP.new('http://localhost:7474'))
end

def get_kml(from, to)
  from_latitude, from_longitude = from.split(',')
  to_latitude, to_longitude = to.split(',')

  neo4j_session.query(
    """
    MATCH (start:Cell{latitude:{from_latitude}, longitude:{from_longitude}})
    WITH start
    MATCH (end:Cell{latitude:{to_latitude}, longitude:{to_longitude}})
    CALL apoc.algo.aStar(start, end, 'LINKED>', 'cost','lat','long') YIELD path
    RETURN path
    """,
    from_latitude: from_latitude,
    from_longitude: from_longitude,
    to_latitude: to_latitude,
    to_longitude: to_longitude
  ).first[:path].nodes.map do |n|
    [n.properties[:latitude], n.properties[:longitude]]
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
