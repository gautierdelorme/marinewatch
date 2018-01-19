require 'sinatra'
require 'sinatra/streaming'
require 'neo4j-core'
require 'neo4j/core/cypher_session/adaptors/http'

def neo4j_session
  @neo4j_session ||= Neo4j::Core::CypherSession.new(Neo4j::Core::CypherSession::Adaptors::HTTP.new('http://localhost:7474'))
end

def get_coordinates
  coordinates = neo4j_session.query(
    """
    MATCH (a:Cell)
    RETURN a.latitude, a.longitude
    ORDER BY rand() LIMIT 1
    """
  ).first
  [coordinates[:'a.latitude'], coordinates[:'a.longitude']]
end

get '/stream' do
  stream do |out|
    while true do
      latitude, longitude = get_coordinates
      out.write "{\"latitude\": \"#{latitude}\",\"longitude\": \"#{longitude}\",\"cell\": #{rand}}\n"
      sleep 10
      out.flush
    end
  end
end
