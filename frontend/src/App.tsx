import { useState } from 'react';
import { GroupSelector } from './components/GroupSelector';
import { GroupView } from './components/GroupView';

function App() {
  const [selectedGroupId, setSelectedGroupId] = useState<number | null>(null);

  if (selectedGroupId === null) {
    return <GroupSelector onSelect={setSelectedGroupId} />;
  }

  return <GroupView groupId={selectedGroupId} onBack={() => setSelectedGroupId(null)} />;
}

export default App;
