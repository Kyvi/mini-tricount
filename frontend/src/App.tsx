import { GroupView } from './components/GroupView';

// TODO: remplacer par un routing quand plusieurs groupes/écrans existeront.
const GROUP_ID = 3;

function App() {
  return <GroupView groupId={GROUP_ID} />;
}

export default App;
